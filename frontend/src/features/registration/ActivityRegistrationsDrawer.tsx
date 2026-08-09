import { useQuery } from '@tanstack/react-query';
import { Drawer, Empty, Input, List, Pagination, Select, Space, Spin, Statistic, Tag } from 'antd';
import { Search, Users, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { ActivitySession } from '../activity/activity-types';
import { getOrganizerRegistrations } from './registration-api';
import type { OrganizerRegistrationQuery, RegistrationStatus } from './registration-types';
import './activity-registrations.css';

const PAGE_SIZE = 20;

export function ActivityRegistrationsDrawer({
  activityId,
  activityTitle,
  sessions,
  open,
  onClose,
}: {
  activityId: number | null;
  activityTitle: string;
  sessions: ActivitySession[];
  open: boolean;
  onClose: () => void;
}) {
  const [sessionId, setSessionId] = useState<number | undefined>();
  const [status, setStatus] = useState<RegistrationStatus | 'ALL'>('CONFIRMED');
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);

  useEffect(() => {
    if (open) {
      setSessionId(undefined);
      setStatus('CONFIRMED');
      setKeywordInput('');
      setKeyword('');
      setPage(1);
    }
  }, [open, activityId]);

  const query: OrganizerRegistrationQuery = {
    sessionId,
    status,
    keyword: keyword || undefined,
    page,
    size: PAGE_SIZE,
  };
  const registrationsQuery = useQuery({
    queryKey: ['activity-registrations', activityId, query],
    queryFn: () => getOrganizerRegistrations(activityId ?? 0, query),
    enabled: open && activityId !== null,
  });
  const data = registrationsQuery.data;

  const submitSearch = () => {
    setKeyword(keywordInput.trim());
    setPage(1);
  };

  return (
    <Drawer
      className="activity-registrations-drawer"
      closeIcon={<X size={18} />}
      destroyOnClose
      onClose={onClose}
      open={open}
      title={
        <div className="activity-registrations-drawer__title">
          <Users size={20} />
          <span>{activityTitle} · 报名管理</span>
        </div>
      }
      width={760}
    >
      <div className="activity-registrations-drawer__surface">
      <div className="activity-registrations-drawer__stats">
        <Statistic title="已报名" value={data?.confirmedCount ?? 0} />
        <Statistic title="已取消" value={data?.cancelledCount ?? 0} />
        <Statistic title="当前结果" value={data?.total ?? 0} />
      </div>

      <div className="activity-registrations-drawer__filters">
        <Select
          aria-label="场次筛选"
          allowClear
          onChange={(value: number | undefined) => {
            setSessionId(value);
            setPage(1);
          }}
          placeholder="全部场次"
          value={sessionId}
          options={sessions.map((session) => ({ label: session.title, value: session.id }))}
        />
        <Select
          aria-label="状态筛选"
          onChange={(value: RegistrationStatus | 'ALL') => {
            setStatus(value);
            setPage(1);
          }}
          value={status}
          options={[
            { label: '已报名', value: 'CONFIRMED' },
            { label: '已取消', value: 'CANCELLED' },
            { label: '全部状态', value: 'ALL' },
          ]}
        />
        <Space.Compact className="activity-registrations-drawer__search">
          <Input
            aria-label="搜索报名人员"
            onChange={(event) => setKeywordInput(event.target.value)}
            onPressEnter={submitSearch}
            placeholder="姓名、用户名、手机号或邮箱"
            value={keywordInput}
          />
          <button aria-label="搜索" onClick={submitSearch} type="button">
            <Search size={16} />
          </button>
        </Space.Compact>
      </div>

      {registrationsQuery.isLoading ? (
        <div className="activity-registrations-drawer__loading">
          <Spin />
        </div>
      ) : null}
      {!registrationsQuery.isLoading && data?.items.length === 0 ? (
        <Empty description="暂时没有符合条件的报名记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : null}
      {data && data.items.length > 0 ? (
        <List
          className="activity-registrations-drawer__list"
          dataSource={data.items}
          renderItem={(registration) => (
            <List.Item>
              <div className="activity-registrations-drawer__participant">
                <div className="activity-registrations-drawer__participant-main">
                  <strong>{registration.displayName}</strong>
                  <span>@{registration.username}</span>
                </div>
                <div className="activity-registrations-drawer__participant-meta">
                  <span>{registration.sessionTitle}</span>
                  <span>{registration.mobile || registration.email || '未填写联系方式'}</span>
                  <span>{formatDateTime(registration.registrationTime)}</span>
                </div>
                <Tag color={registration.status === 'CONFIRMED' ? 'success' : 'default'}>
                  {registration.status === 'CONFIRMED' ? '已报名' : '已取消'}
                </Tag>
              </div>
            </List.Item>
          )}
        />
      ) : null}
      {data && data.total > PAGE_SIZE ? (
        <Pagination
          current={page}
          pageSize={PAGE_SIZE}
          onChange={setPage}
          showSizeChanger={false}
          total={data.total}
        />
      ) : null}
      </div>
    </Drawer>
  );
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value));
}
