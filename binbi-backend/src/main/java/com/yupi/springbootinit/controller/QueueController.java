package com.yupi.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/queue")
@Slf4j
public class QueueController {
    @Resource(name = "thread2")
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name) {
        CompletableFuture.runAsync(() -> {
            System.out.println("任务执行中：" + name + "当前正在执行的线程:" + Thread.currentThread().getName());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },threadPoolExecutor);
    }
    @GetMapping("/get")
    public String get() {
        Map<String,Object> map = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度：",size);
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数",taskCount);
        long completeTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数量：",completeTaskCount);
        int activeTaskCount = threadPoolExecutor.getActiveCount();
        map.put("正在进行的线程数量：",activeTaskCount);

        return JSONUtil.toJsonStr(map);
    }
}
