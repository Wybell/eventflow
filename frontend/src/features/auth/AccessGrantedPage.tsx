import { Button, Typography } from 'antd';
import { ArrowRight, Orbit } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '../../shared/auth/session-store';
import './login-page.css';

export function AccessGrantedPage() {
  const navigate = useNavigate();
  const clearSession = useSessionStore((state) => state.clearSession);

  const handleLogout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };

  return (
    <section className="login-page access-page" aria-labelledby="access-title">
      <div className="login-page__scene" aria-hidden="true" />
      <div className="login-page__content">
        <div className="login-page__identity">
          <div className="login-page__brand-mark" aria-hidden="true">
            <Orbit size={25} strokeWidth={2.4} />
          </div>
          <span>EventFlow</span>
        </div>
        <div className="access-panel">
          <Typography.Title id="access-title" level={1}>
            身份已验证
          </Typography.Title>
          <Typography.Paragraph>你已进入 EventFlow。</Typography.Paragraph>
          <Button icon={<ArrowRight size={18} />} onClick={handleLogout} size="large">
            退出登录
          </Button>
        </div>
      </div>
    </section>
  );
}
