package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMqConstant;
import com.yupi.springbootinit.bizmq.BiProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.FileConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.dto.file.UploadFileRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.FileUploadBizEnum;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource(name = "thread1")
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiProducer biProducer;

    long modelId = BiMqConstant.MODE_ID;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
               getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 上传文件，生成图表
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        User loginUser = userService.getLoginUser(request);
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");

        //文件校验
        String fileName = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR,"文件超过 1MB");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(fileName);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //用户输入
        StringBuilder userInput = new StringBuilder();
        //分析需求
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "请使用" + chartType + "进行分析";
        }
        userInput.append(userGoal).append("\n");
        //从excel提取的压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        //返回结果
        String result = aiManager.doChat(modelId,userInput.toString());
        //拆分返回结果
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"生成格式错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setUserId(loginUser.getId());
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setStatus("succeed");
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR,"图表数据保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 上传文件，生成图表 (线程池异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/AsyncGen")
    public BaseResponse<BiResponse> AsyncGenChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        User loginUser = userService.getLoginUser(request);
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");

        //文件校验
        String fileName = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR,"文件超过 1MB");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(fileName);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //用户输入
        StringBuilder userInput = new StringBuilder();
        //分析需求
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "请使用" + chartType + "进行分析";
        }
        userInput.append(userGoal).append("\n");
        //从excel提取的压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setUserId(loginUser.getId());
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setStatus("wait");
        chartService.save(chart);
        //todo 处理队列满了后，抛异常的情况
        CompletableFuture.runAsync(()-> {
            //先修改图表的任务状态为“执行中”，等执行成功后，再修改为“已完成”，保存执行结果，执行失败后，状态修改为“失败”，记录任务失败信息
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图表生成中状态失败");
                return;
            }
            //调用ai
            String result = aiManager.doChat(modelId,userInput.toString());
            //拆分返回结果
            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(),"AI生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setStatus("succeed");
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }
        },threadPoolExecutor);
        BiResponse biResponse = new BiResponse();
        return ResultUtils.success(biResponse);
    }

    /**
     * 异步消息队列
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/AsyncGen/mq")
    public BaseResponse<BiResponse> GenChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        User loginUser = userService.getLoginUser(request);
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");

        //文件校验
        String fileName = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR,"文件超过 1MB");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(fileName);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());


        String csvData = ExcelUtils.excelToCsv(multipartFile);
        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setUserId(loginUser.getId());
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setStatus("wait");
        chartService.save(chart);
        //给mq发送消息
        biProducer.sendMessage(String.valueOf(chart.getId()));
        BiResponse biResponse = new BiResponse();
        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(long chartId,String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            log.error("更新图表状态失败" + chartId + "," + execMessage);
        }
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id>0,"id",id);
        queryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal",goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType),"chartType",chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq("isDelete",false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
