import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Empty, Input, Modal, Table, Tag, Typography, message } from 'antd';
import { CalendarDays, Check, Compass, LogOut, RadioTower, X } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '../../shared/auth/session-store';
import type { Activity } from '../activity/activity-types';
import { getPendingActivityReviews, reviewActivity } from './activity-review-api';
import './organizer-applications.css';

export function ActivityReviewPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const clearSession = useSessionStore((state) => state.clearSession);
  const [messageApi, contextHolder] = message.useMessage();
  const [reviewTarget, setReviewTarget] = useState<{
    activity: Activity;
    action: 'approve' | 'reject';
  } | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const activitiesQuery = useQuery({
    queryKey: ['activities', 'pending-review'],
    queryFn: getPendingActivityReviews,
  });
  const reviewMutation = useMutation({
    mutationFn: () =>
      reviewActivity(reviewTarget?.activity.id ?? 0, reviewTarget?.action ?? 'reject', reviewNote),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['activities', 'pending-review'] });
      setReviewTarget(null);
      setReviewNote('');
      messageApi.success('审核结果已保存');
    },
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
          <Button icon={<CalendarDays size={17} />} onClick={() => navigate('/my-activities')} type="text">
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
        {activitiesQuery.data?.length === 0 ? (
          <Empty description="暂无待审核活动" />
        ) : (
          <Table<Activity>
            dataSource={activitiesQuery.data}
            loading={activitiesQuery.isLoading}
            pagination={false}
            rowKey="id"
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
                title: '状态',
                render: () => <Tag color="gold">待审核</Tag>,
              },
              {
                title: '操作',
                render: (_, activity) => (
                  <>
                    <Button
                      icon={<Check size={15} />}
                      onClick={() => setReviewTarget({ activity, action: 'approve' })}
                      type="primary"
                    >
                      通过审核
                    </Button>
                    <Button
                      danger
                      icon={<X size={15} />}
                      onClick={() => setReviewTarget({ activity, action: 'reject' })}
                    >
                      驳回
                    </Button>
                  </>
                ),
              },
            ]}
          />
        )}
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
