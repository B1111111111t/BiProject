export default [
  { path: '/user', layout: false, routes: [{ path: '/user/login', component: './User/Login' }] },
  { path: '/', redirect:'/add_chart'},
  { path: '/add_chart',name:'智能分析', icon: 'barChart', component: './AddChart' },
  { path: '/add_chartAsync',name:'智能分析(异步）', icon: 'barChart', component: './AddChartAsync' },
  { path: '/my_chart',name:'我的图表', icon: 'pieChart', component: './MyChartPage' },
  {
    path: '/admin',
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      { path: '/admin', redirect: '/admin/sub-page' },
      { path: '/admin/sub-page', component: './Admin' },
    ],
  },
  { path: '/', redirect: '/welcome' },
  { path: '*', layout: false, component: './404' },
];
