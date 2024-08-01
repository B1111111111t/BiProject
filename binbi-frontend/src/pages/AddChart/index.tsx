
import { Form, Button, Upload, Space, Input, message, Row, Col, Card, Spin } from "antd";
import { Select } from 'antd/lib';
import ReactECharts from 'echarts-for-react';

import React, { useState } from 'react';
import TextArea from 'antd/es/input/TextArea';
import { UploadOutlined } from '@ant-design/icons';
import { genChartByAiUsingPost } from '@/services/binbi/chartController';
import { Divider } from "antd";



const AddChart: React.FC = () => {
  const [chart, setChart] = useState<API.BiResponse>();
  const [submiting, setSubmitting] = useState<boolean>(false);
  const [option, setOption] = useState<any>();

  const onFinish = async (values: any) => {
    // todo 对接后端，上传数据
    if (submiting) {
      return;
    }
    setChart(undefined);
    setOption(undefined);
    setSubmitting(true)
    const params = {
      ...values,
      file: undefined
    }
    try {
      const res = await genChartByAiUsingPost(params, {}, values.file.file.originFileObj)
      if (!res?.data) {
        message.error('分析失败')
      } else {
        const chartOption = JSON.parse(res.data.genChart ?? '')
        if (!chartOption) {
          throw new Error('图表解析错误')
        } else {
          setChart(res.data);
          setOption(chartOption);
          message.success('分析成功')
        }
      }
    } catch (e: any) {
      message.error('分析失败，' + e.message)
    }

    console.log('用户表单内容', values);
    setSubmitting(false);
  };

  return (
    <div className='add-chart'>
      <Row gutter={24}>
        <Col span={12}>
          <Card title="智能分析">
            <Form
              name="addChart"
              onFinish={onFinish}
              initialValues={{}}
              labelAlign="left"
              labelCol={{span:5}}
              wrapperCol={{span:16}} 
            >
              <Form.Item name="goal" label="分析目标" rules={[{ required: true, message: '请输入分析目标！' }]}>
                <TextArea placeholder='请输入您的分析需求'></TextArea>
              </Form.Item>

              <Form.Item name="name" label="图表名称">
                <Input placeholder='请输入您的图表名称'></Input>
              </Form.Item>

              <Form.Item name="chartType" label="图表类型">
                <Select placeholder="请选择图表类型"
                  options={[
                    { value: '折线图', label: '折线图' },
                    { value: '柱状图', label: '柱状图' },
                    { value: '堆叠图', label: '堆叠图' },
                    { value: '树形图', label: '树形图' },
                    { value: '饼图', label: '饼 图' },
                    { value: '雷达图', label: '雷达图' },
                    { value: '热力图', label: '热力图' },
                    { value: '关系图', label: '关系图' },
                    { value: '路径图', label: '路径图' },
                    { value: '散点图', label: '散点图' },
                    { value: '矩形树图', label: '矩形树图' },
                  ]}
                />
              </Form.Item>

              <Form.Item
                name="file"
                label="原始数据"
              >
                <Upload name="file"  maxCount={1}>
                  <Button icon={<UploadOutlined />}>上传 CSV 文件</Button>
                </Upload>
              </Form.Item>

              <Form.Item wrapperCol={{ span: 20, offset: 5 }}>
                <Space>
                  <Button type="primary" htmlType="submit" loading={submiting} disabled={submiting}>
                    提交
                  </Button>
                  <Button htmlType="reset">重置</Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        <Col span={12}>
          <Card title="分析结论">
             {chart?.genResult  ?? <div>请先在左侧提交数据</div>}
             <Spin spinning={submiting} />
          </Card>
          <Divider />
          <Card title="图表生成">
              {
                option ? <ReactECharts option={option} /> : <div>请先在左侧提交数据</div>
              }
              <Spin spinning={submiting} />
          </Card>
        </Col>
      </Row>


    </div>
  );
};
export default AddChart;
