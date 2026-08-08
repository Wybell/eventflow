import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  DatePicker,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Progress,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import {
  CalendarPlus,
  ChevronRight,
  CircleGauge,
  Plus,
  RadioTower,
  Rocket,
  Send,
  ShieldCheck,
  Users,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import { ProfileMenu } from '../profile/ProfileMenu';
import { ActivityRegistrationsDrawer } from '../registration/ActivityRegistrationsDrawer';
import {
  createActivity,
  createActivitySession,
  getActivitySessions,
  getMyActivities,
  publishActivity,
  submitActivityForReview,
  updateActivity,
} from './activity-api';
import type { Activity, ActivityInput, ActivitySessionInput } from './activity-types';
import './activity-workspace.css';

type ActivityFormValues = Omit<ActivityInput, 'registrationStartTime' | 'registrationEndTime'> & {
  registrationWindow: [Dayjs, Dayjs];
};

type SessionFormValues = Omit<ActivitySessionInput, 'startTime' | 'endTime'> & {
  sessionWindow: [Dayjs, Dayjs];
};

export function ActivityWorkspace() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentUser = useSessionStore((state) => state.currentUser);
  const activityQueryKey = ['activities', 'mine', currentUser?.id] as const;
  const [messageApi, contextHolder] = message.useMessage();
  const [isActivityDrawerOpen, setActivityDrawerOpen] = useState(false);
  const [isRegistrationsDrawerOpen, setRegistrationsDrawerOpen] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);
  const [activityForm] = Form.useForm<ActivityFormValues>();
  const [sessionForm] = Form.useForm<SessionFormValues>();

  const activitiesQuery = useQuery({
    queryKey: activityQueryKey,
    queryFn: getMyActivities,
    enabled: currentUser !== null,
  });
  const sessionsQuery = useQuery({
    queryKey: ['activity-sessions', selectedActivity?.id],
    queryFn: () => getActivitySessions(selectedActivity?.id ?? 0),
    enabled: selectedActivity !== null,
  });

  const activityMutation = useMutation({
    mutationFn: async (values: ActivityFormValues) => {
      const input = toActivityInput(values);
      if (selectedActivity === null) {
        await createActivity(input);
      } else {
        await updateActivity(selectedActivity.id, input);
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: activityQueryKey });
      messageApi.success(selectedActivity === null ? '活动草稿已创建' : '活动信息已保存');
      setActivityDrawerOpen(false);
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  const sessionMutation = useMutation({
    mutationFn: (values: SessionFormValues) =>
      createActivitySession(selectedActivity?.id ?? 0, {
        title: values.title,
        startTime: values.sessionWindow[0].format('YYYY-MM-DDTHH:mm:ss'),
        endTime: values.sessionWindow[1].format('YYYY-MM-DDTHH:mm:ss'),
        totalQuota: values.totalQuota,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['activity-sessions', selectedActivity?.id],
      });
      sessionForm.resetFields();
      messageApi.success('场次已加入名额队列');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  const submitMutation = useMutation({
    mutationFn: submitActivityForReview,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: activityQueryKey });
      setSelectedActivity(null);
      messageApi.success('活动已提交审核，审核通过后等待你确认发布');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  const publishMutation = useMutation({
    mutationFn: publishActivity,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: activityQueryKey });
      setSelectedActivity(null);
      messageApi.success('活动已正式发布到活动广场');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  const summary = useMemo(() => {
    const activities = activitiesQuery.data ?? [];
    return {
      total: activities.length,
      pendingReview: activities.filter((activity) => activity.status === 'PENDING_REVIEW').length,
      published: activities.filter((activity) => activity.status === 'PUBLISHED').length,
    };
  }, [activitiesQuery.data]);

  const openCreateDrawer = () => {
    setSelectedActivity(null);
    activityForm.resetFields();
    setActivityDrawerOpen(true);
  };

  const openEditDrawer = (activity: Activity) => {
    setSelectedActivity(activity);
    activityForm.setFieldsValue({
      title: activity.title,
      summary: activity.summary ?? undefined,
      coverUrl: activity.coverUrl ?? undefined,
      venueName: activity.venueName ?? undefined,
      organizerName: activity.organizerName,
      contactName: activity.contactName,
      contactMobile: activity.contactMobile ?? undefined,
      contactEmail: activity.contactEmail ?? undefined,
      registrationWindow: [
        dayjs(activity.registrationStartTime),
        dayjs(activity.registrationEndTime),
      ],
    });
    setActivityDrawerOpen(true);
  };

  const handleSubmit = (activity: Activity) => {
    if (submitMutation.isPending) {
      return;
    }
    submitMutation.mutate(activity.id);
  };

  const handlePublish = (activity: Activity) => {
    if (publishMutation.isPending) {
      return;
    }
    publishMutation.mutate(activity.id);
  };

  return (
    <section className="activity-workspace" aria-label="我的活动">
      {contextHolder}
      <header className="activity-workspace__header">
        <button
          className="activity-workspace__brand"
          type="button"
          onClick={() => navigate('/events')}
        >
          <span className="activity-workspace__brand-mark" aria-hidden="true">
            <RadioTower size={24} />
          </span>
          <span>
            <strong>EventFlow</strong>
            <small>我的活动</small>
          </span>
        </button>
        <div className="activity-workspace__header-actions">
          {currentUser?.roles.includes('ADMIN') ? (
            <Tooltip title="活动审核中心">
              <Button
                icon={<ShieldCheck size={18} />}
                onClick={() => navigate('/admin/activity-reviews')}
              >
                审核中心
              </Button>
            </Tooltip>
          ) : null}
          <Tooltip title="活动广场">
            <Button onClick={() => navigate('/events')}>活动广场</Button>
          </Tooltip>
          <Tooltip title="创建活动">
            <Button icon={<CalendarPlus size={18} />} onClick={openCreateDrawer} type="primary">
              发布活动
            </Button>
          </Tooltip>
          <ProfileMenu />
        </div>
      </header>

      <main className="activity-workspace__main">
        <section className="activity-workspace__intro" aria-labelledby="activity-title">
          <div>
            <Typography.Title id="activity-title" level={1}>
              发布你的活动
            </Typography.Title>
            <Typography.Paragraph>
              {currentUser?.displayName ?? '你'}可以创建活动草稿、设置场次与名额，并提交平台审核。
            </Typography.Paragraph>
          </div>
          <div className="activity-workspace__metrics" aria-label="活动状态汇总">
            <Metric icon={<CircleGauge size={19} />} label="全部活动" value={summary.total} />
            <Metric icon={<Send size={19} />} label="审核中" value={summary.pendingReview} />
            <Metric icon={<Rocket size={19} />} label="已发布" value={summary.published} />
          </div>
        </section>

        <section className="activity-workspace__console" aria-label="活动列表">
          <div className="activity-workspace__console-head">
            <div>
              <span>活动工作区</span>
              <Typography.Title level={2}>管理草稿与审核状态</Typography.Title>
            </div>
            <Button icon={<Plus size={17} />} onClick={openCreateDrawer} type="primary">
              新建活动
            </Button>
          </div>

          {activitiesQuery.isLoading ? <LoadingState /> : null}
          {activitiesQuery.isError ? <ErrorState error={activitiesQuery.error} /> : null}
          {activitiesQuery.data?.length === 0 ? <EmptyState onCreate={openCreateDrawer} /> : null}
          {activitiesQuery.data && activitiesQuery.data.length > 0 ? (
            <div className="activity-workspace__activity-list">
              {activitiesQuery.data.map((activity) => (
                <ActivityRow
                  activity={activity}
                  isPublishDisabled={publishMutation.isPending}
                  isPublishing={
                    publishMutation.isPending && publishMutation.variables === activity.id
                  }
                  isSubmitDisabled={submitMutation.isPending}
                  isSubmitting={
                    submitMutation.isPending && submitMutation.variables === activity.id
                  }
                  key={activity.id}
                  onEdit={() => openEditDrawer(activity)}
                  onManageRegistrations={() => {
                    setSelectedActivity(activity);
                    setRegistrationsDrawerOpen(true);
                  }}
                  onPublish={() => handlePublish(activity)}
                  onSelect={() => setSelectedActivity(activity)}
                  onSubmit={() => handleSubmit(activity)}
                />
              ))}
            </div>
          ) : null}
        </section>
      </main>

      <Drawer
        className="activity-workspace__drawer"
        closeIcon={null}
        destroyOnClose
        onClose={() => setActivityDrawerOpen(false)}
        open={isActivityDrawerOpen}
        title={selectedActivity === null ? '创建活动草稿' : '编辑活动信息'}
        width={520}
      >
        <Form<ActivityFormValues>
          form={activityForm}
          layout="vertical"
          onFinish={(values) => activityMutation.mutate(values)}
          requiredMark={false}
        >
          <Form.Item
            label="活动名称"
            name="title"
            rules={[{ required: true, message: '请输入活动名称' }]}
          >
            <Input maxLength={120} placeholder="例如：2026 开发者峰会" size="large" />
          </Form.Item>
          <Form.Item
            label="主办名称"
            name="organizerName"
            rules={[{ required: true, message: '请输入主办名称' }]}
          >
            <Input maxLength={120} placeholder="例如：EventFlow 技术社区" size="large" />
          </Form.Item>
          <Form.Item
            label="联系人"
            name="contactName"
            rules={[{ required: true, message: '请输入联系人' }]}
          >
            <Input maxLength={64} placeholder="用于审核与活动咨询" size="large" />
          </Form.Item>
          <Form.Item label="联系电话" name="contactMobile">
            <Input maxLength={20} placeholder="选填" size="large" />
          </Form.Item>
          <Form.Item
            label="联系邮箱"
            name="contactEmail"
            rules={[{ type: 'email', message: '请输入正确的邮箱地址' }]}
          >
            <Input maxLength={255} placeholder="选填" size="large" />
          </Form.Item>
          <Form.Item label="活动简介" name="summary">
            <Input.TextArea
              maxLength={500}
              placeholder="说明活动主题、面向人群和关键信息"
              rows={4}
            />
          </Form.Item>
          <Form.Item label="举办地点" name="venueName">
            <Input maxLength={120} placeholder="例如：上海国际会议中心" size="large" />
          </Form.Item>
          <Form.Item label="封面地址" name="coverUrl">
            <Input maxLength={500} placeholder="https://" size="large" />
          </Form.Item>
          <Form.Item
            label="报名开放区间"
            name="registrationWindow"
            rules={[{ required: true, message: '请选择报名时间' }]}
          >
            <DatePicker.RangePicker
              allowClear={false}
              className="activity-workspace__range-picker"
              format="YYYY-MM-DD HH:mm"
              showTime
              size="large"
            />
          </Form.Item>
          <Space className="activity-workspace__drawer-actions">
            <Button onClick={() => setActivityDrawerOpen(false)}>取消</Button>
            <Button htmlType="submit" loading={activityMutation.isPending} type="primary">
              {selectedActivity === null ? '创建草稿' : '保存修改'}
            </Button>
          </Space>
        </Form>
      </Drawer>

      <Drawer
        className="activity-workspace__drawer"
        closeIcon={null}
        destroyOnClose
        onClose={() => setSelectedActivity(null)}
        open={selectedActivity !== null && !isActivityDrawerOpen && !isRegistrationsDrawerOpen}
        title={selectedActivity?.title ?? '活动详情'}
        width={620}
      >
        {selectedActivity !== null ? (
          <section className="activity-workspace__detail">
            <div className="activity-workspace__detail-status">
              <StatusTag status={selectedActivity.status} />
              <span>
                {formatDateRange(
                  selectedActivity.registrationStartTime,
                  selectedActivity.registrationEndTime,
                )}
              </span>
            </div>
            {selectedActivity.reviewNote ? (
              <Typography.Paragraph className="activity-workspace__review-note">
                审核说明：{selectedActivity.reviewNote}
              </Typography.Paragraph>
            ) : null}
            <Typography.Paragraph>
              {selectedActivity.summary || '尚未填写活动简介。'}
            </Typography.Paragraph>
            <div className="activity-workspace__session-heading">
              <div>
                <span>名额控制</span>
                <Typography.Title level={3}>场次与配额</Typography.Title>
              </div>
              {selectedActivity.status === 'PUBLISHED' || selectedActivity.status === 'OFFLINE' ? (
                <Button
                  icon={<Users size={16} />}
                  onClick={() => setRegistrationsDrawerOpen(true)}
                >
                  报名管理
                </Button>
              ) : null}
            </div>
            {canConfigureSessions(selectedActivity) ? (
              <Form<SessionFormValues>
                className="activity-workspace__session-form"
                form={sessionForm}
                layout="vertical"
                onFinish={(values) => sessionMutation.mutate(values)}
                requiredMark={false}
              >
                <Form.Item
                  label="场次名称"
                  name="title"
                  rules={[{ required: true, message: '请输入场次名称' }]}
                >
                  <Input placeholder="例如：主论坛 A" />
                </Form.Item>
                <Form.Item
                  label="开始与结束时间"
                  name="sessionWindow"
                  rules={[{ required: true, message: '请选择场次时间' }]}
                >
                  <DatePicker.RangePicker
                    allowClear={false}
                    className="activity-workspace__range-picker"
                    format="MM-DD HH:mm"
                    showTime
                  />
                </Form.Item>
                <Form.Item
                  label="总名额"
                  name="totalQuota"
                  rules={[{ required: true, message: '请输入总名额' }]}
                >
                  <InputNumber min={1} placeholder="80" style={{ width: '100%' }} />
                </Form.Item>
                <Button
                  htmlType="submit"
                  icon={<Plus size={16} />}
                  loading={sessionMutation.isPending}
                  type="primary"
                >
                  添加场次
                </Button>
              </Form>
            ) : null}
            <List
              className="activity-workspace__session-list"
              dataSource={sessionsQuery.data ?? []}
              loading={sessionsQuery.isLoading}
              locale={{ emptyText: '尚未配置场次' }}
              renderItem={(session) => (
                <List.Item>
                  <div className="activity-workspace__session-item">
                    <div>
                      <strong>{session.title}</strong>
                      <span>{formatDateRange(session.startTime, session.endTime)}</span>
                    </div>
                    <div className="activity-workspace__quota">
                      <span>{session.availableQuota} 个可用名额</span>
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
          </section>
        ) : null}
      </Drawer>
      <ActivityRegistrationsDrawer
        activityId={selectedActivity?.id ?? null}
        activityTitle={selectedActivity?.title ?? ''}
        onClose={() => {
          setRegistrationsDrawerOpen(false);
          setSelectedActivity(null);
        }}
        open={isRegistrationsDrawerOpen}
        sessions={sessionsQuery.data ?? []}
      />
    </section>
  );
}

function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <div className="activity-workspace__metric">
      <span aria-hidden="true">{icon}</span>
      <div>
        <strong>{value}</strong>
        <small>{label}</small>
      </div>
    </div>
  );
}

function ActivityRow({
  activity,
  isPublishDisabled,
  isPublishing,
  isSubmitDisabled,
  isSubmitting,
  onEdit,
  onManageRegistrations,
  onPublish,
  onSubmit,
  onSelect,
}: {
  activity: Activity;
  isPublishDisabled: boolean;
  isPublishing: boolean;
  isSubmitDisabled: boolean;
  isSubmitting: boolean;
  onEdit: () => void;
  onManageRegistrations: () => void;
  onPublish: () => void;
  onSubmit: () => void;
  onSelect: () => void;
}) {
  const canEdit = activity.status === 'DRAFT' || activity.status === 'REJECTED';
  return (
    <article className="activity-workspace__activity-row">
      <button className="activity-workspace__activity-main" onClick={onSelect} type="button">
        <span className="activity-workspace__activity-index">
          {String(activity.id).padStart(2, '0')}
        </span>
        <span className="activity-workspace__activity-copy">
          <strong>{activity.title}</strong>
          <small>
            {formatDateRange(activity.registrationStartTime, activity.registrationEndTime)}
          </small>
        </span>
        <StatusTag status={activity.status} />
        <ChevronRight aria-hidden="true" size={18} />
      </button>
      <div className="activity-workspace__row-actions">
        {activity.status === 'PUBLISHED' || activity.status === 'OFFLINE' ? (
          <Button icon={<Users size={15} />} onClick={onManageRegistrations}>
            报名管理
          </Button>
        ) : null}
        {canEdit ? <Button onClick={onEdit}>编辑</Button> : null}
        {canEdit ? (
          <Button
            disabled={isSubmitDisabled}
            icon={<Send size={15} />}
            loading={isSubmitting}
            onClick={onSubmit}
            type="primary"
          >
            提交审核
          </Button>
        ) : null}
        {activity.status === 'APPROVED' ? (
          <Button
            disabled={isPublishDisabled}
            icon={<Rocket size={15} />}
            loading={isPublishing}
            onClick={onPublish}
            type="primary"
          >
            正式发布
          </Button>
        ) : null}
      </div>
    </article>
  );
}

function StatusTag({ status }: { status: Activity['status'] }) {
  const labels: Record<Activity['status'], string> = {
    DRAFT: '草稿待配置',
    PENDING_REVIEW: '审核中',
    APPROVED: '审核通过，待发布',
    REJECTED: '已驳回，待修改',
    PUBLISHED: '已公开发布',
    OFFLINE: '已下架',
  };
  return (
    <Tag
      className={`activity-workspace__status activity-workspace__status--${status.toLowerCase()}`}
    >
      {labels[status]}
    </Tag>
  );
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <Empty description="还没有活动草稿" image={Empty.PRESENTED_IMAGE_SIMPLE}>
      <Button icon={<Plus size={17} />} onClick={onCreate} type="primary">
        创建第一个活动
      </Button>
    </Empty>
  );
}

function LoadingState() {
  return (
    <div className="activity-workspace__loading">
      <Spin />
    </div>
  );
}

function ErrorState({ error }: { error: unknown }) {
  const errorMessage =
    typeof error === 'object' &&
    error !== null &&
    'message' in error &&
    typeof error.message === 'string'
      ? error.message
      : '加载活动数据失败';
  return <Empty description={errorMessage} image={Empty.PRESENTED_IMAGE_SIMPLE} />;
}

function canConfigureSessions(activity: Activity): boolean {
  return activity.status === 'DRAFT' || activity.status === 'REJECTED';
}

function toActivityInput(values: ActivityFormValues): ActivityInput {
  return {
    title: values.title,
    summary: values.summary,
    coverUrl: values.coverUrl,
    venueName: values.venueName,
    organizerName: values.organizerName,
    contactName: values.contactName,
    contactMobile: values.contactMobile,
    contactEmail: values.contactEmail,
    registrationStartTime: values.registrationWindow[0].format('YYYY-MM-DDTHH:mm:ss'),
    registrationEndTime: values.registrationWindow[1].format('YYYY-MM-DDTHH:mm:ss'),
  };
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
