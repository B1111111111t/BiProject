
import { Form, Button, Upload, Space, Input, message, Card,  } from "antd";
import { Select } from 'antd/lib';

import React, { useState } from 'react';
import TextArea from 'antd/es/input/TextArea';
import { UploadOutlined } from '@ant-design/icons';
import { asyncGenChartByAiUsingPost, genChartByAiAsyncMqUsingPost } from '@/services/binbi/chartController';
import { useForm } from "antd/es/form/Form";



const AddChartAsync: React.FC = () => {
  const [submiting, setSubmitting] = useState<boolean>(false);
  const [form]  = useForm();


  const onFinish = async (values: any) => {
    // todo 对接后端，上传数据
    if (submiting) {
      return;
    }
    setSubmitting(true)
    const params = {
      ...values,
      file: undefined
    }
    try {
      // const res = await asyncGenChartByAiUsingPost(params, {}, values.file.file.originFileObj)
      const res = await genChartByAiAsyncMqUsingPost(params, {}, values.file.file.originFileObj)
      if (!res?.data) {
        message.error('分析失败')
      } else {
          message.success('分析任务提交成功，请稍后在我的图表页面中查看')
          form.resetFields();
        }
      } catch (e: any) {
      message.error('分析失败，' + e.message)
    }

    console.log('用户表单内容', values);
    setSubmitting(false);
  };

  return (
    <div className='add-chart-async'>
      <Card title="智能分析">
        <Form
          form={form}
          name="addChart"
          onFinish={onFinish}
          initialValues={{}}
          labelAlign="left"
          labelCol={{ span: 5 }}
          wrapperCol={{ span: 16 }}
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
            <Upload name="file" maxCount={1}>
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
    </div>
  );
};
export default AddChartAsync;
