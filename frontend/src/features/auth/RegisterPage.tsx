import { useMutation } from '@tanstack/react-query';
import { Button, Form, Input, Typography, message } from 'antd';
import { ArrowLeft, Eye, EyeOff, LockKeyhole, UserPlus, UserRound } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import { getCurrentUser, login, register } from './auth-api';
import { defaultRouteForUser } from './auth-routing';
import type { RegisterInput } from './auth-types';
import './login-page.css';

export function RegisterPage() {
  const navigate = useNavigate();
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const registerMutation = useMutation({ mutationFn: register });

  const handleSubmit = async (values: RegisterInput) => {
    try {
      await registerMutation.mutateAsync(values);
      const tokens = await login({ username: values.username, password: values.password });
      useSessionStore.getState().setAccessToken(tokens.accessToken);
      const user = await getCurrentUser();
      useSessionStore.getState().setSession(tokens.accessToken, tokens.refreshToken, user);
      messageApi.success('账号已创建，现在可以报名或发布活动');
      navigate(defaultRouteForUser(user), { replace: true });
    } catch (error) {
      const apiError = error as ApiError;
      messageApi.error(apiError.message ?? '注册失败，请稍后重试');
    }
  };

  return (
    <section className="login-page register-page" aria-labelledby="register-title">
      {contextHolder}
      <div className="login-page__scene" aria-hidden="true" />
      <div className="login-page__content">
        <div className="login-page__identity">
          <div className="login-page__brand-mark" aria-hidden="true">
            <img src="/icons/eventflow-192.png" alt="" />
          </div>
          <span>EventFlow</span>
        </div>
        <Form<RegisterInput>
          className="login-panel register-panel"
          layout="vertical"
          requiredMark={false}
          onFinish={handleSubmit}
        >
          <button className="register-panel__back" type="button" onClick={() => navigate('/login')}>
            <ArrowLeft size={16} />
            返回登录
          </button>
          <div className="login-panel__heading">
            <Typography.Title id="register-title" level={1}>
              注册 EventFlow 账号
            </Typography.Title>
            <Typography.Paragraph>
              创建一个账号即可报名、预约，也可以创建活动并提交平台审核。
            </Typography.Paragraph>
          </div>
          <div className="register-panel__fields">
            <Form.Item
              label="账号"
              name="username"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input prefix={<UserRound size={17} />} placeholder="6 至 64 位账号" />
            </Form.Item>
            <Form.Item
              label="姓名或称呼"
              name="displayName"
              rules={[{ required: true, message: '请输入姓名或称呼' }]}
            >
              <Input placeholder="用于活动与通知展示" />
            </Form.Item>
            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, min: 6, message: '密码至少 6 位' }]}
            >
              <Input.Password
                iconRender={(visible) => (visible ? <EyeOff size={17} /> : <Eye size={17} />)}
                prefix={<LockKeyhole size={17} />}
                placeholder="至少 6 位"
                visibilityToggle={{
                  visible: passwordVisible,
                  onVisibleChange: setPasswordVisible,
                }}
              />
            </Form.Item>
            <Form.Item label="手机号" name="mobile">
              <Input placeholder="选填" />
            </Form.Item>
            <Form.Item
              label="邮箱"
              name="email"
              rules={[{ type: 'email', message: '请输入正确的邮箱地址' }]}
            >
              <Input placeholder="选填" />
            </Form.Item>
          </div>
          <Button
            block
            className="login-panel__submit"
            htmlType="submit"
            icon={<UserPlus size={18} />}
            loading={registerMutation.isPending}
            size="large"
            type="primary"
          >
            创建账号
          </Button>
        </Form>
      </div>
    </section>
  );
}
