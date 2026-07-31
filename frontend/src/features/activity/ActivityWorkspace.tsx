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
  Modal,
  Progress,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { Dayjs } from 'dayjs';
import {
  CalendarPlus,
  ChevronRight,
  CircleGauge,
  LogOut,
  Plus,
  RadioTower,
  Rocket,
  Users,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ApiError } from '../../shared/api/api-contract';
import { useSessionStore } from '../../shared/auth/session-store';
import {
  createActivity,
  createActivitySession,
  getActivitySessions,
  getMyActivities,
  publishActivity,
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

const ACTIVITY_QUERY_KEY = ['activities', 'mine'] as const;

export function ActivityWorkspace() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const clearSession = useSessionStore((state) => state.clearSession);
  const [messageApi, contextHolder] = message.useMessage();
  const [isActivityDrawerOpen, setActivityDrawerOpen] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);
  const [activityForm] = Form.useForm<ActivityFormValues>();
  const [sessionForm] = Form.useForm<SessionFormValues>();

  const activitiesQuery = useQuery({ queryKey: ACTIVITY_QUERY_KEY, queryFn: getMyActivities });
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
      await queryClient.invalidateQueries({ queryKey: ACTIVITY_QUERY_KEY });
      messageApi.success(selectedActivity === null ? '活动草稿已创建' : '活动信息已更新');
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

  const publishMutation = useMutation({
    mutationFn: publishActivity,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ACTIVITY_QUERY_KEY });
      messageApi.success('活动已发布');
    },
    onError: (error: ApiError) => messageApi.error(error.message),
  });

  const summary = useMemo(() => {
    const activities = activitiesQuery.data ?? [];
    return {
      total: activities.length,
      published: activities.filter((activity) => activity.status === 'PUBLISHED').length,
      draft: activities.filter((activity) => activity.status === 'DRAFT').length,
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
    });
    setActivityDrawerOpen(true);
  };

  const confirmPublish = (activity: Activity) => {
    Modal.confirm({
      title: '确认发布活动？',
      content: '发布后活动基础信息不可再编辑，请确认报名时间和场次设置已完成。',
      okText: '发布活动',
      cancelText: '暂不发布',
      onOk: () => publishMutation.mutateAsync(activity.id),
    });
  };

  const handleLogout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };

  return (
    <section className="activity-workspace" aria-label="活动管理工作台">
      {contextHolder}
      <header className="activity-workspace__header">
        <button
          className="activity-workspace__brand"
          type="button"
          onClick={() => setSelectedActivity(null)}
        >
          <span className="activity-workspace__brand-mark" aria-hidden="true">
            <RadioTower size={24} />
          </span>
          <span>
            <strong>EventFlow</strong>
            <small>活动管理控制台</small>
          </span>
        </button>
        <div className="activity-workspace__header-actions">
          <Tooltip title="创建活动">
            <Button icon={<CalendarPlus size={18} />} onClick={openCreateDrawer} type="primary">
              创建活动
            </Button>
          </Tooltip>
          <Tooltip title="退出登录">
            <Button
              aria-label="退出登录"
              icon={<LogOut size={18} />}
              onClick={handleLogout}
              type="text"
            />
          </Tooltip>
        </div>
      </header>

      <main className="activity-workspace__main">
        <section className="activity-workspace__intro" aria-labelledby="activity-title">
          <div>
            <Typography.Title id="activity-title" level={1}>
              活动指挥中心
            </Typography.Title>
            <Typography.Paragraph>
              从活动草稿、场次配额到发布状态，在一个工作区内完成管理。
            </Typography.Paragraph>
          </div>
          <div className="activity-workspace__metrics" aria-label="活动状态汇总">
            <Metric icon={<CircleGauge size={19} />} label="全部活动" value={summary.total} />
            <Metric icon={<Rocket size={19} />} label="已发布" value={summary.published} />
            <Metric icon={<Users size={19} />} label="待配置" value={summary.draft} />
          </div>
        </section>

        <section className="activity-workspace__console" aria-label="活动列表">
          <div className="activity-workspace__console-head">
            <div>
              <span>活动编队</span>
              <Typography.Title level={2}>管理活动与名额</Typography.Title>
            </div>
            <Button icon={<Plus size={17} />} onClick={openCreateDrawer} type="primary">
              新建草稿
            </Button>
          </div>

          {activitiesQuery.isLoading ? (
            <div className="activity-workspace__loading">
              <Spin />
            </div>
          ) : null}
          {activitiesQuery.isError ? <ErrorState error={activitiesQuery.error} /> : null}
          {activitiesQuery.data?.length === 0 ? <EmptyState onCreate={openCreateDrawer} /> : null}
          {activitiesQuery.data && activitiesQuery.data.length > 0 ? (
            <div className="activity-workspace__activity-list">
              {activitiesQuery.data.map((activity) => (
                <ActivityRow
                  activity={activity}
                  onEdit={() => openEditDrawer(activity)}
                  onPublish={() => confirmPublish(activity)}
                  onSelect={() => setSelectedActivity(activity)}
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
        title={selectedActivity === null ? '建立活动草稿' : '编辑活动基础信息'}
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
        open={selectedActivity !== null && !isActivityDrawerOpen}
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
            <Typography.Paragraph>
              {selectedActivity.summary || '尚未填写活动简介。'}
            </Typography.Paragraph>
            <div className="activity-workspace__session-heading">
              <div>
                <span>名额控制</span>
                <Typography.Title level={3}>场次与配额</Typography.Title>
              </div>
              {selectedActivity.status === 'DRAFT' ? (
                <Button
                  icon={<Plus size={16} />}
                  onClick={() => sessionForm.submit()}
                  type="primary"
                >
                  添加场次
                </Button>
              ) : null}
            </div>
            {selectedActivity.status === 'DRAFT' ? (
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
  onEdit,
  onPublish,
  onSelect,
}: {
  activity: Activity;
  onEdit: () => void;
  onPublish: () => void;
  onSelect: () => void;
}) {
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
        {activity.status === 'DRAFT' ? (
          <>
            <Button onClick={onEdit}>编辑</Button>
            <Button onClick={onPublish} type="primary">
              发布
            </Button>
          </>
        ) : null}
      </div>
    </article>
  );
}

function StatusTag({ status }: { status: Activity['status'] }) {
  const labels: Record<Activity['status'], string> = {
    DRAFT: '草稿配置中',
    PUBLISHED: '报名已开放',
    OFFLINE: '已下线',
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

function ErrorState({ error }: { error: unknown }) {
  const message =
    typeof error === 'object' &&
    error !== null &&
    'message' in error &&
    typeof error.message === 'string'
      ? error.message
      : '加载活动数据失败';
  return <Empty description={message} image={Empty.PRESENTED_IMAGE_SIMPLE} />;
}

function toActivityInput(values: ActivityFormValues): ActivityInput {
  return {
    title: values.title,
    summary: values.summary,
    coverUrl: values.coverUrl,
    venueName: values.venueName,
    registrationStartTime: values.registrationWindow[0].format('YYYY-MM-DDTHH:mm:ss'),
    registrationEndTime: values.registrationWindow[1].format('YYYY-MM-DDTHH:mm:ss'),
  };
}

function formatDateRange(start: string, end: string): string {
  const startValue = new Date(start);
  const endValue = new Date(end);
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  return `${formatter.format(startValue)} 至 ${formatter.format(endValue)}`;
}
