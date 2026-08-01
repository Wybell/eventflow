import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Empty, Input, Modal, Table, Tag, Typography, message } from 'antd';
import { Check, LogOut, RadioTower, X } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '../../shared/auth/session-store';
import {
  getOrganizerApplications,
  reviewOrganizerApplication,
  type OrganizerApplication,
} from './organizer-application-api';
import './organizer-applications.css';

export function OrganizerApplicationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const clearSession = useSessionStore((state) => state.clearSession);
  const [messageApi, contextHolder] = message.useMessage();
  const [reviewTarget, setReviewTarget] = useState<{
    application: OrganizerApplication;
    action: 'approve' | 'reject';
  } | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const applicationsQuery = useQuery({
    queryKey: ['organizer-applications'],
    queryFn: getOrganizerApplications,
  });
  const reviewMutation = useMutation({
    mutationFn: () =>
      reviewOrganizerApplication(
        reviewTarget?.application.id ?? 0,
        reviewTarget?.action ?? 'reject',
        reviewNote,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['organizer-applications'] });
      setReviewTarget(null);
      setReviewNote('');
      messageApi.success('审核结果已保存');
    },
  });
  return (
    <section className="admin-applications">
      <>{contextHolder}</>
      <header>
        <div>
          <RadioTower size={22} />
          <strong>EventFlow 管理中心</strong>
        </div>
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
      </header>
      <main>
        <Typography.Title level={1}>主办方资质审核</Typography.Title>
        <Typography.Paragraph>审核通过后将创建组织，并授予活动发布权限。</Typography.Paragraph>
        {applicationsQuery.data?.length === 0 ? (
          <Empty description="暂无待审核申请" />
        ) : (
          <Table<OrganizerApplication>
            dataSource={applicationsQuery.data}
            pagination={false}
            rowKey="id"
            columns={[
              { title: '组织名称', dataIndex: 'organizationName' },
              {
                title: '申请时间',
                dataIndex: 'createTime',
                render: (value) => new Date(value).toLocaleString('zh-CN'),
              },
              {
                title: '状态',
                dataIndex: 'status',
                render: (status) => (
                  <Tag color="gold">{status === 'PENDING' ? '待审核' : status}</Tag>
                ),
              },
              {
                title: '操作',
                render: (_, application) => (
                  <>
                    <Button
                      icon={<Check size={15} />}
                      onClick={() => setReviewTarget({ application, action: 'approve' })}
                      type="primary"
                    >
                      通过
                    </Button>
                    <Button
                      danger
                      icon={<X size={15} />}
                      onClick={() => setReviewTarget({ application, action: 'reject' })}
                    >
                      拒绝
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
        okText={reviewTarget?.action === 'approve' ? '确认通过' : '确认拒绝'}
        onCancel={() => setReviewTarget(null)}
        onOk={() => reviewMutation.mutate()}
        open={reviewTarget !== null}
        title={reviewTarget?.action === 'approve' ? '确认通过主办方申请？' : '确认拒绝主办方申请？'}
      >
        <Input.TextArea
          onChange={(event) => setReviewNote(event.target.value)}
          placeholder="审核说明，可选"
          rows={3}
          value={reviewNote}
        />
      </Modal>
    </section>
  );
}
