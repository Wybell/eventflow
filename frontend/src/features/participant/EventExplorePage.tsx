import { useQuery } from '@tanstack/react-query';
import { Button, Drawer, Empty, List, Progress, Spin, Tag, Typography } from 'antd';
import { CalendarDays, MapPin, RadioTower, ShieldCheck, TicketCheck } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPublishedActivities, getPublishedActivitySessions } from '../activity/activity-api';
import type { Activity } from '../activity/activity-types';
import { ProfileMenu } from '../profile/ProfileMenu';
import { useSessionStore } from '../../shared/auth/session-store';
import './event-explore.css';

export function EventExplorePage() {
  const navigate = useNavigate();
  const currentUser = useSessionStore((state) => state.currentUser);
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);
  const activitiesQuery = useQuery({
    queryKey: ['activities', 'public'],
    queryFn: getPublishedActivities,
  });
  const sessionsQuery = useQuery({
    queryKey: ['activity-sessions', 'public', selectedActivity?.id],
    queryFn: () => getPublishedActivitySessions(selectedActivity?.id ?? 0),
    enabled: selectedActivity !== null,
  });

  return (
    <section className="event-explore" aria-label="活动发现">
      <header className="event-explore__header">
        <div className="event-explore__brand">
          <RadioTower size={23} />
          <strong>EventFlow</strong>
        </div>
        <div className="event-explore__account">
          <ProfileMenu />
          {currentUser?.roles.includes('ADMIN') ? (
            <Button
              icon={<ShieldCheck size={17} />}
              onClick={() => navigate('/admin/activity-reviews')}
              type="text"
            >
              审核中心
            </Button>
          ) : null}
          <Button onClick={() => navigate('/my-activities')} type="text">
            发布活动
          </Button>
        </div>
      </header>
      <main className="event-explore__main">
        <section className="event-explore__intro">
          <div>
            <Typography.Title level={1}>探索值得到场的活动</Typography.Title>
            <Typography.Paragraph>发现公开活动，查看场次和实时可用名额。</Typography.Paragraph>
          </div>
          <Tag className="event-explore__identity">活动广场</Tag>
        </section>
        <section className="event-explore__rail">
          {activitiesQuery.isLoading ? (
            <div className="event-explore__loading">
              <Spin />
            </div>
          ) : null}
          {activitiesQuery.data?.length === 0 ? (
            <Empty description="暂时没有公开活动" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : null}
          {activitiesQuery.data?.map((activity) => (
            <ActivityCard
              activity={activity}
              key={activity.id}
              onSelect={() => setSelectedActivity(activity)}
            />
          ))}
        </section>
      </main>
      <Drawer
        className="event-explore__drawer"
        closeIcon={null}
        onClose={() => setSelectedActivity(null)}
        open={selectedActivity !== null}
        title={selectedActivity?.title}
        width={560}
      >
        {selectedActivity ? (
          <>
            <Typography.Paragraph>
              {selectedActivity.summary || '主办方暂未填写活动简介。'}
            </Typography.Paragraph>
            <div className="event-explore__detail-meta">
              <MapPin size={16} />
              {selectedActivity.venueName || '地点待定'}
            </div>
            <Typography.Title level={3}>可选场次</Typography.Title>
            <List
              dataSource={sessionsQuery.data ?? []}
              loading={sessionsQuery.isLoading}
              locale={{ emptyText: '主办方暂未发布可预约场次' }}
              renderItem={(session) => (
                <List.Item>
                  <div className="event-explore__session">
                    <div>
                      <strong>{session.title}</strong>
                      <span>
                        {formatDate(session.startTime)} 至 {formatDate(session.endTime)}
                      </span>
                    </div>
                    <div>
                      <b>{session.availableQuota}</b>
                      <small> 个可用名额</small>
                      <Progress
                        percent={Math.round((session.availableQuota / session.totalQuota) * 100)}
                        showInfo={false}
                        size="small"
                      />
                    </div>
                  </div>
                </List.Item>
              )}
            />
          </>
        ) : null}
      </Drawer>
    </section>
  );
}

function ActivityCard({ activity, onSelect }: { activity: Activity; onSelect: () => void }) {
  return (
    <article className="event-explore__card">
      <div className="event-explore__card-index">{String(activity.id).padStart(2, '0')}</div>
      <div className="event-explore__card-copy">
        <Typography.Title level={2}>{activity.title}</Typography.Title>
        <p>{activity.summary || '面向活动参与者开放报名预约。'}</p>
        <div>
          <span>
            <CalendarDays size={15} />
            {formatDate(activity.registrationStartTime)} 开放
          </span>
          <span>
            <MapPin size={15} />
            {activity.venueName || '地点待定'}
          </span>
        </div>
      </div>
      <Button icon={<TicketCheck size={17} />} onClick={onSelect} type="primary">
        查看场次
      </Button>
    </article>
  );
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value));
}
