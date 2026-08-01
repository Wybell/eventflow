import { useMutation } from '@tanstack/react-query';
import { Button, Form, Input, Segmented, Typography, message } from 'antd';
import { ArrowLeft, Building2, LockKeyhole, Orbit, UserPlus, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import { getCurrentUser, login, register } from './auth-api';
import { defaultRouteForUser } from './auth-routing';
import type { RegisterInput, RegistrationType } from './auth-types';
import './login-page.css';

type RegisterFormValues = Omit<RegisterInput, 'registrationType'>;

export function RegisterPage() {
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const [registrationType, setRegistrationType] = useState<RegistrationType>('PARTICIPANT');
  const [form] = Form.useForm<RegisterFormValues>();
  const registerMutation = useMutation({ mutationFn: register });

  const handleSubmit = async (values: RegisterFormValues) => {
    try {
      await registerMutation.mutateAsync({ ...values, registrationType });
      const tokens = await login({ username: values.username, password: values.password });
      useSessionStore.getState().setAccessToken(tokens.accessToken);
      const user = await getCurrentUser();
      useSessionStore.getState().setSession(tokens.accessToken, user);
      messageApi.success(
        registrationType === 'ORGANIZER' ? '申请已提交，审核期间可使用报名视图。' : '账号已创建。',
      );
      navigate(defaultRouteForUser(user), { replace: true });
    } catch (error) {
      const apiError = error as ApiError;
      messageApi.error(apiError.message ?? '注册失败，请稍后重试。');
    }
  };

  return (
    <section className="login-page register-page" aria-labelledby="register-title">
      {contextHolder}
      <div className="login-page__scene" aria-hidden="true" />
      <div className="login-page__content">
        <div className="login-page__identity">
          <div className="login-page__brand-mark" aria-hidden="true">
            <Orbit size={25} strokeWidth={2.4} />
          </div>
          <span>EventFlow</span>
        </div>
        <Form<RegisterFormValues>
          className="login-panel register-panel"
          form={form}
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
              选择你的使用身份，后续可在一个账号中切换不同视图。
            </Typography.Paragraph>
          </div>
          <Segmented<RegistrationType>
            block
            className="register-panel__type"
            onChange={setRegistrationType}
            options={[
              { label: '报名人员', value: 'PARTICIPANT', icon: <UserRound size={15} /> },
              { label: '主办方', value: 'ORGANIZER', icon: <Building2 size={15} /> },
            ]}
            value={registrationType}
          />
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
              rules={[{ required: true, min: 8, message: '密码至少 8 位' }]}
            >
              <Input.Password prefix={<LockKeyhole size={17} />} placeholder="至少 8 位" />
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
            {registrationType === 'ORGANIZER' ? (
              <>
                <Form.Item
                  label="组织名称"
                  name="organizationName"
                  rules={[{ required: true, message: '请输入组织名称' }]}
                >
                  <Input prefix={<Building2 size={17} />} placeholder="例如：EventFlow 技术社区" />
                </Form.Item>
                <Form.Item
                  label="联系人"
                  name="contactName"
                  rules={[{ required: true, message: '请输入联系人' }]}
                >
                  <Input placeholder="审核联系使用" />
                </Form.Item>
                <Form.Item label="组织简介" name="organizationDescription">
                  <Input.TextArea rows={3} maxLength={500} placeholder="选填" />
                </Form.Item>
              </>
            ) : null}
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
            {registrationType === 'ORGANIZER' ? '提交主办方申请' : '创建账号'}
          </Button>
        </Form>
      </div>
    </section>
  );
}
