import { HomeOutlined } from '@ant-design/icons'
import { Layout, Menu, Typography } from 'antd'
import { Link, Outlet, useLocation } from 'react-router-dom'

export function AppLayout() {
  const { pathname } = useLocation()

  return (
    <Layout className="app-layout">
      <Layout.Header className="app-header">
        <Link to="/" className="brand">{import.meta.env.VITE_APP_TITLE || 'DedsiArche'}</Link>
        <Menu
          mode="horizontal"
          selectedKeys={[pathname]}
          items={[{ key: '/', icon: <HomeOutlined />, label: <Link to="/">首页</Link> }]}
        />
      </Layout.Header>
      <Layout.Content className="app-content"><Outlet /></Layout.Content>
      <Layout.Footer className="app-footer">
        <Typography.Text type="secondary">始于本原，成于至简。</Typography.Text>
      </Layout.Footer>
    </Layout>
  )
}
