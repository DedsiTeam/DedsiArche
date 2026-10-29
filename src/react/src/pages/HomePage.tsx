import { Card, Col, Row, Space, Tag, Typography } from 'antd'

const foundations = [
  { title: '页面与路由', description: '在 pages 中添加页面，在 router 中登记路由。' },
  { title: '界面与布局', description: '使用 Ant Design 组件，在 layouts 中维护公共布局。' },
  { title: '接口与服务', description: '在 services 中组织接口调用，复用统一的 Axios 客户端。' },
]

export function HomePage() {
  return (
    <Space orientation="vertical" size={32} style={{ width: '100%' }}>
      <section className="welcome">
        <Typography.Text className="eyebrow">DEDSI ARCHE</Typography.Text>
        <Typography.Title>从这里开始构建。</Typography.Title>
        <Typography.Paragraph type="secondary">
          清晰的结构，独立的前端，为接下来的业务留出空间。
        </Typography.Paragraph>
        <Space wrap>
          {['React', 'TypeScript', 'Vite', 'Ant Design'].map((name) => <Tag key={name}>{name}</Tag>)}
        </Space>
      </section>
      <Row gutter={[20, 20]}>
        {foundations.map((item) => (
          <Col xs={24} md={8} key={item.title}>
            <Card title={item.title} className="foundation-card">
              <Typography.Paragraph type="secondary">{item.description}</Typography.Paragraph>
            </Card>
          </Col>
        ))}
      </Row>
    </Space>
  )
}
