import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Drawer, Empty, List, Progress, Spin, Tag, Typography, message } from 'antd';
import {
  CalendarDays,
  ClipboardList,
  MapPin,
  RadioTower,
  ShieldCheck,
  TicketCheck,
  XCircle,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPublishedActivities, getPublishedActivitySessions } from '../activity/activity-api';
import type { Activity, ActivitySession } from '../activity/activity-types';
import { ProfileMenu } from '../profile/ProfileMenu';
import {
  cancelRegistration,
  getMyRegistrations,
  registerForActivity,
} from '../registration/registration-api';
import type { ActivityRegistration } from '../registration/registration-types';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import './event-explore.css';

export function EventExplorePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentUser = useSessionStore((state) => state.currentUser);
  const [messageApi, contextHolder] = message.useMessage();
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);
  const [isRegistrationsOpen, setRegistrationsOpen] = useState(false);
  const registrationQueryKey = ['registrations', 'mine', currentUser?.id] as const;

  const activitiesQuery = useQuery({
    queryKey: ['activities', 'public'],
    queryFn: getPublishedActivities,
  });
  const sessionsQuery = useQuery({
    queryKey: ['activity-sessions', 'public', selectedActivity?.id],
    queryFn: () => getPublishedActivitySessions(selectedActivity?.id ?? 0),
    enabled: selectedActivity !== null,
  });
  const registrationsQuery = useQuery({
    queryKey: registrationQueryKey,
    queryFn: getMyRegistrations,
    enabled: currentUser !== null,
  });
  const activeRegistrations = useMemo(
    () =>
      new Map(
        (registrationsQuery.data ?? [])
          .filter((registration) => registration.status === 'CONFIRMED')
          .map((registration) => [registration.activityId, registration]),
      ),
    [registrationsQuery.data],
  );

  const registrationMutation = useMutation({
    mutationFn: registerForActivity,
    onSuccess: async (_, input) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: registrationQueryKey }),
        queryClient.invalidateQueries({
          queryKey: ['activity-sessions', 'public', input.activityId],
        }),
      ]);
      messageApi.success('报名成功，已加入“我的报名”');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });
  const cancellationMutation = useMutation({
    mutationFn: (registration: ActivityRegistration) => cancelRegistration(registration.id),
    onSuccess: async (_, registration) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: registrationQueryKey }),
        queryClient.invalidateQueries({
          queryKey: ['activity-sessions', 'public', registration.activityId],
        }),
      ]);
      messageApi.success('报名已取消，名额已归还');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  return (
    <section className="event-explore" aria-label="活动发现">
      {contextHolder}
      <header className="event-explore__header">
        <div className="event-explore__brand">
          <RadioTower size={23} />
          <strong>EventFlow</strong>
        </div>
        <div className="event-explore__account">
          <ProfileMenu />
          <Button
            icon={<ClipboardList size={17} />}
            onClick={() => setRegistrationsOpen(true)}
            type="text"
          >
            我的报名
          </Button>
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
        onClose={() => setSelectedActivity(null)}
        open={selectedActivity !== null}
        title={selectedActivity?.title}
        width={600}
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
              locale={{ emptyText: '主办方暂未发布可报名场次' }}
              renderItem={(session) => (
                <SessionItem
                  activeRegistration={activeRegistrations.get(selectedActivity.id)}
                  activity={selectedActivity}
                  isSubmitting={
                    registrationMutation.isPending &&
                    registrationMutation.variables?.sessionId === session.id
                  }
                  onRegister={() =>
                    registrationMutation.mutate({
                      activityId: selectedActivity.id,
                      sessionId: session.id,
                    })
                  }
                  session={session}
                />
              )}
            />
          </>
        ) : null}
      </Drawer>
      <Drawer
        className="event-explore__drawer"
        onClose={() => setRegistrationsOpen(false)}
        open={isRegistrationsOpen}
        title="我的报名"
        width={520}
      >
        <List
          dataSource={registrationsQuery.data ?? []}
          loading={registrationsQuery.isLoading}
          locale={{ emptyText: '还没有报名记录' }}
          renderItem={(registration) => (
            <RegistrationItem
              isCancelling={
                cancellationMutation.isPending &&
                cancellationMutation.variables?.id === registration.id
              }
              onCancel={() => cancellationMutation.mutate(registration)}
              registration={registration}
            />
          )}
        />
      </Drawer>
    </section>
  );
}

function SessionItem({
  activeRegistration,
  activity,
  isSubmitting,
  onRegister,
  session,
}: {
  activeRegistration?: ActivityRegistration;
  activity: Activity;
  isSubmitting: boolean;
  onRegister: () => void;
  session: ActivitySession;
}) {
  const action = getSessionAction(activity, session, activeRegistration);

  return (
    <List.Item>
      <div className="event-explore__session">
        <div className="event-explore__session-copy">
          <strong>{session.title}</strong>
          <span>
            {formatDate(session.startTime)} 至 {formatDate(session.endTime)}
          </span>
        </div>
        <div className="event-explore__session-quota">
          <span>
            <b>{session.availableQuota}</b>
            <small> 个可用名额</small>
          </span>
          <Progress
            percent={Math.round((session.availableQuota / session.totalQuota) * 100)}
            showInfo={false}
            size="small"
          />
        </div>
        <Button
          disabled={action.disabled}
          loading={isSubmitting}
          onClick={onRegister}
          type={action.disabled ? 'default' : 'primary'}
        >
          {action.label}
        </Button>
      </div>
    </List.Item>
  );
}

function RegistrationItem({
  isCancelling,
  onCancel,
  registration,
}: {
  isCancelling: boolean;
  onCancel: () => void;
  registration: ActivityRegistration;
}) {
  const isConfirmed = registration.status === 'CONFIRMED';
  return (
    <List.Item>
      <div className="event-explore__registration">
        <div className="event-explore__registration-heading">
          <strong>{registration.activityTitle}</strong>
          <Tag color={isConfirmed ? 'success' : 'default'}>
            {isConfirmed ? '报名成功' : '已取消'}
          </Tag>
        </div>
        <span>{registration.sessionTitle}</span>
        <span>
          {formatDate(registration.sessionStartTime)} 至 {formatDate(registration.sessionEndTime)}
        </span>
        <span className="event-explore__registration-venue">
          <MapPin size={14} />
          {registration.venueName || '地点待定'}
        </span>
        {isConfirmed ? (
          <Button danger icon={<XCircle size={16} />} loading={isCancelling} onClick={onCancel}>
            取消报名
          </Button>
        ) : null}
      </div>
    </List.Item>
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

function getSessionAction(
  activity: Activity,
  session: ActivitySession,
  activeRegistration?: ActivityRegistration,
): { disabled: boolean; label: string } {
  if (activeRegistration) {
    return activeRegistration.sessionId === session.id
      ? { disabled: true, label: '已报名' }
      : { disabled: true, label: '已选其他场次' };
  }
  const now = Date.now();
  if (now < new Date(activity.registrationStartTime).getTime()) {
    return { disabled: true, label: '报名未开始' };
  }
  if (now >= new Date(activity.registrationEndTime).getTime()) {
    return { disabled: true, label: '报名已结束' };
  }
  if (now >= new Date(session.startTime).getTime()) {
    return { disabled: true, label: '场次已开始' };
  }
  if (session.availableQuota <= 0) {
    return { disabled: true, label: '名额已满' };
  }
  return { disabled: false, label: '报名此场次' };
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
