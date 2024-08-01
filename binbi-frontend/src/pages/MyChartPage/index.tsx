


import { listMyChartByPageUsingPost } from '@/services/binbi/chartController';
import { Card, Result, message } from 'antd';
import React, { useEffect, useState } from 'react';
import { List } from 'antd';
import ReactECharts from 'echarts-for-react';
import { Input } from 'antd';

const { Search } = Input;





const MyChartPage: React.FC = () => {
  const initSearchParams = {
    current: 1,
    pageSize: 8,
    sortField:'createtime',
    sortOrder:'desc'
  }
  const [searchParams, setSearchParams] = useState<API.ChartQueryRequest>({ ...initSearchParams });
  const [chartList, setChartList] = useState<API.Chart[]>();
  const [total, setTotal] = useState<number>(0);
  const loadData = async () => {
    try {
      const res = await listMyChartByPageUsingPost(searchParams);
      //隐藏图表title
      if (res.data) {
        setChartList(res.data.records ?? []);
        setTotal(res.data.total ?? 0);
        if (res.data.records) {
          res.data.records?.forEach(data => {
            if(data.status === 'succeed'){
              const chartOption = JSON.parse(data.genChart ?? '{}')
              chartOption.title = undefined;
              data.genChart = JSON.stringify(chartOption);
            }
          })
        }
      } else {
        message.error('获取我的图表失败')
      }
    } catch (e: any) {
      message.error('获取图表失败', e.message)
    }
  }
  useEffect(() => {
    loadData();
  }, [searchParams])
  return (
    <div className='my-chart-page'>
      <div style={{ marginBottom: 16 }}>
        <Search placeholder="请输入图表名称" onSearch={(value) => {
          setSearchParams({
            ...initSearchParams,
            name: value,
          })
        }} enterButton />
      </div>

      <List
        grid={{
          gutter: 16,
          xs: 1,
          sm: 1,
          md: 1,
          lg: 1,
          xl: 2,
          xxl: 2,
        }}
        pagination={{
          onChange: (page, pageSize) => {
            setSearchParams({
              ...searchParams,
              current: page,
              pageSize,
            })
          },
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total: total
        }}
        dataSource={chartList}

        renderItem={(item) => (

          <List.Item
            key={item.id}
          >
            <Card>
              <List.Item.Meta
                title={'图表名称：' + item.name}
                description={item.chartType ? '图表类型：' + item.chartType : undefined}
              />
              {
                item.status === 'wait' &&
                <>
                  <Result
                    status='warning'
                    title = '图表待生成'
                    subTitle = {item.execMessage ?? '系统繁忙，请耐心等待'}
                  />
                </>
              }
              {
                item.status === 'running' &&
                <>
                  <Result
                    status='info'
                    title = '图表生成中'
                    subTitle = {item.execMessage}
                  />
                </>
              }
              {
                item.status === 'succeed' &&
                <>
                  {'分析目标：' + item.goal}
                  <div style={{ marginBottom: 16 }}></div>
                  {<ReactECharts option={JSON.parse(item.genChart ?? '{}')} />}
                </>
              }
              
              {
                item.status === 'failed' &&
                <>
                  <Result
                    status='error'
                    title = '图表生成失败'
                    subTitle = {item.execMessage}
                  />
                </>
              }
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
};
export default MyChartPage;
