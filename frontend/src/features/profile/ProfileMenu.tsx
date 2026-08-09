import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Avatar,
  Button,
  Drawer,
  Dropdown,
  Form,
  Input,
  Tabs,
  Tag,
  Typography,
  Upload,
  message,
  type MenuProps,
  type UploadProps,
} from 'antd';
import { Camera, ChevronDown, KeyRound, LogOut, UserRound } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import { changePassword, getCurrentUser, updateProfile, uploadAvatar } from '../auth/auth-api';
import type { ChangePasswordInput, UpdateProfileInput } from '../auth/auth-types';
import './profile-menu.css';

interface PasswordFormValues extends ChangePasswordInput {
  confirmPassword: string;
}

export function ProfileMenu() {
  const navigate = useNavigate();
  const currentUser = useSessionStore((state) => state.currentUser);
  const updateCurrentUser = useSessionStore((state) => state.updateCurrentUser);
  const clearSession = useSessionStore((state) => state.clearSession);
  const [messageApi, contextHolder] = message.useMessage();
  const [isDrawerOpen, setDrawerOpen] = useState(false);
  const [profileForm] = Form.useForm<UpdateProfileInput>();
  const [passwordForm] = Form.useForm<PasswordFormValues>();
  const needsProfileHydration =
    currentUser !== null &&
    (currentUser.avatarUrl === undefined ||
      currentUser.mobile === undefined ||
      currentUser.email === undefined);
  const currentUserQuery = useQuery({
    queryKey: ['auth', 'me', currentUser?.id],
    queryFn: getCurrentUser,
    enabled: isDrawerOpen && needsProfileHydration,
  });

  useEffect(() => {
    if (currentUserQuery.data !== undefined) {
      updateCurrentUser(currentUserQuery.data);
    }
  }, [currentUserQuery.data, updateCurrentUser]);

  useEffect(() => {
    if (currentUser !== null && isDrawerOpen) {
      profileForm.setFieldsValue({
        displayName: currentUser.displayName,
        mobile: currentUser.mobile ?? undefined,
        email: currentUser.email ?? undefined,
      });
    }
  }, [currentUser, isDrawerOpen, profileForm]);

  const profileMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (user) => {
      updateCurrentUser(user);
      messageApi.success('个人资料已保存');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });
  const avatarMutation = useMutation({
    mutationFn: uploadAvatar,
    onSuccess: (avatarUrl) => {
      if (currentUser !== null) {
        updateCurrentUser({ ...currentUser, avatarUrl });
      }
      messageApi.success('头像已更新');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });
  const passwordMutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      messageApi.success('密码已修改，请重新登录');
      passwordForm.resetFields();
      clearSession();
      navigate('/login', { replace: true });
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  if (currentUser === null) {
    return null;
  }

  const handleLogout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };
  const menuItems: MenuProps['items'] = [
    { key: 'profile', icon: <UserRound size={16} />, label: '个人资料' },
    { type: 'divider' },
    { key: 'logout', icon: <LogOut size={16} />, label: '退出登录', danger: true },
  ];
  const uploadProps: UploadProps = {
    accept: 'image/jpeg,image/png,image/webp',
    maxCount: 1,
    showUploadList: false,
    customRequest: ({ file, onError, onSuccess }) => {
      avatarMutation.mutate(file as File, {
        onSuccess: () => onSuccess?.({}),
        onError: (error) => onError?.(new Error(error.message)),
      });
    },
    beforeUpload: (file) => {
      if (file.size > 5 * 1024 * 1024) {
        messageApi.error('头像图片不能超过 5MB');
        return Upload.LIST_IGNORE;
      }
      return true;
    },
  };

  return (
    <>
      {contextHolder}
      <Dropdown
        menu={{
          items: menuItems,
          onClick: ({ key }) => {
            if (key === 'profile') setDrawerOpen(true);
            if (key === 'logout') handleLogout();
          },
        }}
        placement="bottomRight"
        trigger={['click']}
      >
        <button
          aria-label={`${currentUser.displayName}，打开个人菜单`}
          className="profile-trigger"
          type="button"
        >
          <UserAvatar avatarUrl={currentUser.avatarUrl} size={34} />
          <span>{currentUser.displayName}</span>
          <ChevronDown size={15} />
        </button>
      </Dropdown>
      <Drawer
        className="profile-drawer"
        destroyOnClose={false}
        onClose={() => setDrawerOpen(false)}
        open={isDrawerOpen}
        title="个人中心"
        width={620}
      >
        <div className="profile-drawer__surface">
        <section className="profile-summary">
          <div className="profile-avatar-wrap">
            <UserAvatar avatarUrl={currentUser.avatarUrl} size={88} />
            <Upload {...uploadProps}>
              <Button
                aria-label="更换头像"
                className="profile-avatar-edit"
                icon={<Camera size={16} />}
                loading={avatarMutation.isPending}
                shape="circle"
              />
            </Upload>
          </div>
          <div>
            <Typography.Title level={2}>{currentUser.displayName}</Typography.Title>
            <Typography.Text>@{currentUser.username}</Typography.Text>
            <div className="profile-roles">
              {currentUser.roles.map((role) => (
                <Tag key={role}>{roleLabel(role)}</Tag>
              ))}
            </div>
          </div>
        </section>
        <Tabs
          items={[
            {
              key: 'profile',
              label: '基本资料',
              children: (
                <Form<UpdateProfileInput>
                  form={profileForm}
                  layout="vertical"
                  onFinish={(values) => profileMutation.mutate(values)}
                  requiredMark={false}
                >
                  <Form.Item label="登录账号">
                    <Input disabled value={currentUser.username} />
                  </Form.Item>
                  <Form.Item
                    label="姓名或称呼"
                    name="displayName"
                    rules={[{ required: true, message: '请输入姓名或称呼' }]}
                  >
                    <Input maxLength={64} />
                  </Form.Item>
                  <Form.Item label="手机号" name="mobile">
                    <Input maxLength={20} placeholder="选填" />
                  </Form.Item>
                  <Form.Item
                    label="邮箱"
                    name="email"
                    rules={[{ type: 'email', message: '请输入正确的邮箱地址' }]}
                  >
                    <Input maxLength={255} placeholder="选填" />
                  </Form.Item>
                  <Button htmlType="submit" loading={profileMutation.isPending} type="primary">
                    保存资料
                  </Button>
                </Form>
              ),
            },
            {
              key: 'security',
              icon: <KeyRound size={15} />,
              label: '账号安全',
              children: (
                <Form<PasswordFormValues>
                  form={passwordForm}
                  layout="vertical"
                  onFinish={({ currentPassword, newPassword }) =>
                    passwordMutation.mutate({ currentPassword, newPassword })
                  }
                  requiredMark={false}
                >
                  <Form.Item
                    label="当前密码"
                    name="currentPassword"
                    rules={[{ required: true, message: '请输入当前密码' }]}
                  >
                    <Input.Password autoComplete="current-password" />
                  </Form.Item>
                  <Form.Item
                    label="新密码"
                    name="newPassword"
                    rules={[
                      { required: true, message: '请输入新密码' },
                      { min: 8, message: '新密码至少 8 位' },
                    ]}
                  >
                    <Input.Password autoComplete="new-password" />
                  </Form.Item>
                  <Form.Item
                    dependencies={['newPassword']}
                    label="确认新密码"
                    name="confirmPassword"
                    rules={[
                      { required: true, message: '请再次输入新密码' },
                      ({ getFieldValue }) => ({
                        validator: (_, value) =>
                          !value || getFieldValue('newPassword') === value
                            ? Promise.resolve()
                            : Promise.reject(new Error('两次输入的新密码不一致')),
                      }),
                    ]}
                  >
                    <Input.Password autoComplete="new-password" />
                  </Form.Item>
                  <Button htmlType="submit" loading={passwordMutation.isPending} type="primary">
                    修改密码
                  </Button>
                </Form>
              ),
            },
          ]}
        />
        </div>
      </Drawer>
    </>
  );
}

function UserAvatar({ avatarUrl, size }: { avatarUrl: string | null; size: number }) {
  return (
    <Avatar
      className="profile-avatar"
      icon={avatarUrl ? undefined : <UserRound size={Math.round(size * 0.56)} />}
      size={size}
      src={avatarUrl ?? undefined}
    />
  );
}

function roleLabel(role: string): string {
  if (role === 'ADMIN') return '管理员';
  if (role === 'ORGANIZER') return '组织者';
  return '普通用户';
}
