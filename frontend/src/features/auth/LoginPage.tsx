import { useMutation } from '@tanstack/react-query';
import { Button, Form, Input, Typography } from 'antd';
import { Eye, EyeOff, LockKeyhole, LogIn, UserPlus, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '../../shared/auth/session-store';
import type { ApiError } from '../../shared/api/api-contract';
import { getCurrentUser, login } from './auth-api';
import { defaultRouteForUser } from './auth-routing';
import type { LoginInput } from './auth-types';
import './login-page.css';

export function LoginPage() {
  const navigate = useNavigate();
  const [passwordVisible, setPasswordVisible] = useState(false);
  const setSession = useSessionStore((state) => state.setSession);
  const loginMutation = useMutation({ mutationFn: login });

  const handleSubmit = async (values: LoginInput) => {
    try {
      const tokens = await loginMutation.mutateAsync(values);
      useSessionStore.getState().setAccessToken(tokens.accessToken);
      const user = await getCurrentUser();
      setSession(tokens.accessToken, tokens.refreshToken, user);
      navigate(defaultRouteForUser(user), { replace: true });
    } catch {
      // The form renders the normalized server error below the submit action.
    }
  };

  const error = loginMutation.error as ApiError | null;

  return (
    <section className="login-page" aria-labelledby="login-title">
      <div className="login-page__scene" aria-hidden="true" />
      <div className="login-page__content">
        <div className="login-page__identity">
          <div className="login-page__brand-mark" aria-hidden="true">
            <img src="/icons/eventflow-192.png" alt="" />
          </div>
          <span>EventFlow</span>
        </div>
        <Form<LoginInput>
          className="login-panel"
          layout="vertical"
          requiredMark={false}
          onFinish={handleSubmit}
        >
          <div className="login-panel__heading">
            <Typography.Title id="login-title" level={1}>
              进入活动入口
            </Typography.Title>
            <Typography.Paragraph>使用你的账号继续访问活动报名与名额预约。</Typography.Paragraph>
          </div>
          <Form.Item
            label="账号"
            name="username"
            rules={[{ required: true, message: '请输入账号' }]}
          >
            <Input
              autoComplete="username"
              prefix={<UserRound size={17} />}
              placeholder="输入账号"
              size="large"
            />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              autoComplete="current-password"
              iconRender={(visible) => (visible ? <EyeOff size={17} /> : <Eye size={17} />)}
              prefix={<LockKeyhole size={17} />}
              placeholder="输入密码"
              size="large"
              visibilityToggle={{
                visible: passwordVisible,
                onVisibleChange: setPasswordVisible,
              }}
            />
          </Form.Item>
          {error ? <p className="login-panel__error">{error.message}</p> : null}
          <Button
            block
            className="login-panel__submit"
            htmlType="submit"
            icon={<LogIn size={18} />}
            loading={loginMutation.isPending}
            size="large"
            type="primary"
          >
            登录
          </Button>
          <button
            className="login-panel__register"
            type="button"
            onClick={() => navigate('/register')}
          >
            <UserPlus size={16} />
            注册账号
          </button>
        </Form>
      </div>
    </section>
  );
}
