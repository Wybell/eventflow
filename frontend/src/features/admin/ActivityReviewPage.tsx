import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Empty, Input, Modal, Table, Tabs, Tag, Typography, message } from 'antd';
import { CalendarDays, Check, Compass, LogOut, RadioTower, X } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import type { Activity } from '../activity/activity-types';
import {
  getPendingActivityReviews,
  getReviewedActivities,
  reviewActivity,
} from './activity-review-api';
import './organizer-applications.css';

const STATUS_PRESENTATION: Record<Activity['status'], { color: string; label: string }> = {
  DRAFT: { color: 'default', label: '草稿' },
  PENDING_REVIEW: { color: 'gold', label: '待审核' },
  APPROVED: { color: 'blue', label: '审核通过，待发布' },
  REJECTED: { color: 'red', label: '已驳回' },
  PUBLISHED: { color: 'green', label: '已发布' },
  OFFLINE: { color: 'default', label: '已下架' },
};

export function ActivityReviewPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const clearSession = useSessionStore((state) => state.clearSession);
  const currentUser = useSessionStore((state) => state.currentUser);
  const [messageApi, contextHolder] = message.useMessage();
  const [reviewTarget, setReviewTarget] = useState<{
    activity: Activity;
    action: 'approve' | 'reject';
  } | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const pendingReviewQueryKey = ['activities', 'admin', 'pending-review', currentUser?.id] as const;
  const reviewedActivitiesQueryKey = ['activities', 'admin', 'reviewed', currentUser?.id] as const;
  const activitiesQuery = useQuery({
    queryKey: pendingReviewQueryKey,
    queryFn: getPendingActivityReviews,
    enabled: currentUser !== null,
  });
  const reviewedActivitiesQuery = useQuery({
    queryKey: reviewedActivitiesQueryKey,
    queryFn: getReviewedActivities,
    enabled: currentUser !== null,
  });
  const reviewMutation = useMutation({
    mutationFn: () =>
      reviewActivity(reviewTarget?.activity.id ?? 0, reviewTarget?.action ?? 'reject', reviewNote),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: pendingReviewQueryKey }),
        queryClient.invalidateQueries({ queryKey: reviewedActivitiesQueryKey }),
      ]);
      setReviewTarget(null);
      setReviewNote('');
      messageApi.success('审核结果已保存');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  return (
    <section className="admin-applications">
      {contextHolder}
      <header>
        <div>
          <RadioTower size={22} />
          <strong>EventFlow 管理中心</strong>
        </div>
        <div className="admin-applications__header-actions">
          <Button icon={<Compass size={17} />} onClick={() => navigate('/events')} type="text">
            活动广场
          </Button>
          <Button
            icon={<CalendarDays size={17} />}
            onClick={() => navigate('/my-activities')}
            type="text"
          >
            我的活动
          </Button>
          <Button
            icon={<LogOut size={17} />}
            onClick={() => {
              clearSession();
              navigate('/login', { replace: true });
            }}
            type="text"
          >
            退出
          </Button>
        </div>
      </header>
      <main>
        <Typography.Title level={1}>活动审核中心</Typography.Title>
        <Typography.Paragraph>审核通过后，活动发起者确认后手动发布。</Typography.Paragraph>
        <Tabs
          className="admin-applications__tabs"
          items={[
            {
              key: 'pending',
              label: `待审核 (${activitiesQuery.data?.length ?? 0})`,
              children: (
                <ActivityReviewTable
                  activities={activitiesQuery.data ?? []}
                  emptyText="暂无待审核活动"
                  loading={activitiesQuery.isLoading}
                  onApprove={(activity) => setReviewTarget({ activity, action: 'approve' })}
                  onReject={(activity) => setReviewTarget({ activity, action: 'reject' })}
                />
              ),
            },
            {
              key: 'reviewed',
              label: `我的审核记录 (${reviewedActivitiesQuery.data?.length ?? 0})`,
              children: (
                <ActivityReviewTable
                  activities={reviewedActivitiesQuery.data ?? []}
                  emptyText="暂无审核记录"
                  loading={reviewedActivitiesQuery.isLoading}
                />
              ),
            },
          ]}
        />
      </main>
      <Modal
        cancelText="取消"
        okButtonProps={{
          danger: reviewTarget?.action === 'reject',
          loading: reviewMutation.isPending,
        }}
        okText={reviewTarget?.action === 'approve' ? '确认通过' : '确认驳回'}
        onCancel={() => setReviewTarget(null)}
        onOk={() => {
          if (reviewTarget?.action === 'reject' && reviewNote.trim().length === 0) {
            messageApi.error('驳回活动时必须填写审核原因');
            return;
          }
          reviewMutation.mutate();
        }}
        open={reviewTarget !== null}
        title={reviewTarget?.action === 'approve' ? '确认通过此活动？' : '确认驳回此活动？'}
      >
        <Input.TextArea
          onChange={(event) => setReviewNote(event.target.value)}
          placeholder={reviewTarget?.action === 'reject' ? '请填写驳回原因' : '审核说明，可选'}
          rows={3}
          value={reviewNote}
        />
      </Modal>
    </section>
  );
}

function ActivityReviewTable({
  activities,
  emptyText,
  loading,
  onApprove,
  onReject,
}: {
  activities: Activity[];
  emptyText: string;
  loading: boolean;
  onApprove?: (activity: Activity) => void;
  onReject?: (activity: Activity) => void;
}) {
  if (!loading && activities.length === 0) {
    return <Empty description={emptyText} />;
  }

  return (
    <Table<Activity>
      columns={[
        { title: '活动名称', dataIndex: 'title' },
        { title: '主办名称', dataIndex: 'organizerName' },
        { title: '联系人', dataIndex: 'contactName' },
        {
          title: '报名时间',
          render: (_, activity) =>
            formatDateRange(activity.registrationStartTime, activity.registrationEndTime),
        },
        {
          title: '当前状态',
          render: (_, activity) => <ActivityStatusTag status={activity.status} />,
        },
        ...(onApprove && onReject
          ? [
              {
                title: '操作',
                render: (_: unknown, activity: Activity) => (
                  <>
                    <Button
                      icon={<Check size={15} />}
                      onClick={() => onApprove(activity)}
                      type="primary"
                    >
                      通过审核
                    </Button>
                    <Button danger icon={<X size={15} />} onClick={() => onReject(activity)}>
                      驳回
                    </Button>
                  </>
                ),
              },
            ]
          : []),
      ]}
      dataSource={activities}
      loading={loading}
      pagination={false}
      rowKey="id"
    />
  );
}

function ActivityStatusTag({ status }: { status: Activity['status'] }) {
  const presentation = STATUS_PRESENTATION[status];
  return <Tag color={presentation.color}>{presentation.label}</Tag>;
}

function formatDateRange(start: string, end: string): string {
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  return `${formatter.format(new Date(start))} 至 ${formatter.format(new Date(end))}`;
}
