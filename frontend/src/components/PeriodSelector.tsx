import { Button, Dropdown, MenuProps, Modal, Form, Input, message } from 'antd';
import { PlusOutlined, DeleteOutlined, CheckOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import { usePeriodStore } from '@/stores/periodStore';
import {
  getEvaluationPeriods,
  createEvaluationPeriod,
  deleteEvaluationPeriod,
  type EvaluationPeriod,
} from '@/services/evaluationPeriods';

export default function PeriodSelector() {
  const { currentPeriod, setCurrentPeriod } = usePeriodStore();
  const [periods, setPeriods] = useState<EvaluationPeriod[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();

  const loadPeriods = async () => {
    try {
      setLoading(true);
      const data = await getEvaluationPeriods();
      setPeriods(data);
    } catch (error: any) {
      message.error(error.message || '加载批次失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPeriods();
  }, []);

  const handleCreatePeriod = async (values: { name: string }) => {
    try {
      const newPeriod = await createEvaluationPeriod(values);
      message.success('批次创建成功');
      setIsModalOpen(false);
      form.resetFields();
      await loadPeriods();
      setCurrentPeriod(newPeriod);
    } catch (error: any) {
      message.error(error.message || '创建批次失败');
    }
  };

  const handleDeletePeriod = async (id: number) => {
    if (currentPeriod?.id === id) {
      message.error('不能删除当前选中的批次');
      return;
    }
    try {
      await deleteEvaluationPeriod(id);
      message.success('批次删除成功');
      await loadPeriods();
    } catch (error: any) {
      message.error(error.message || '删除批次失败');
    }
  };

  const menuItems: MenuProps['items'] = periods.map((period) => ({
    key: period.id,
    label: (
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', minWidth: 200 }}>
        <span>
          {period.name}
          {currentPeriod?.id === period.id && <CheckOutlined style={{ marginLeft: 8, color: '#1890ff' }} />}
        </span>
        {periods.length > 1 && currentPeriod?.id !== period.id && (
          <DeleteOutlined
            onClick={(e) => {
              e.stopPropagation();
              handleDeletePeriod(period.id);
            }}
            style={{ color: '#ff4d4f' }}
          />
        )}
      </div>
    ),
    onClick: () => setCurrentPeriod(period),
  }));

  menuItems.push({
    type: 'divider',
  });

  menuItems.push({
    key: 'create',
    label: (
      <span>
        <PlusOutlined /> 新建批次
      </span>
    ),
    onClick: () => setIsModalOpen(true),
  });

  return (
    <>
      <Dropdown menu={{ items: menuItems }} trigger={['click']} disabled={loading}>
        <Button>{currentPeriod ? currentPeriod.name : '选择批次'}</Button>
      </Dropdown>

      <Modal
        title="新建测评批次"
        open={isModalOpen}
        onCancel={() => {
          setIsModalOpen(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} onFinish={handleCreatePeriod} layout="vertical">
          <Form.Item
            name="name"
            label="批次名称"
            rules={[
              { required: true, message: '请输入批次名称' },
              { max: 255, message: '批次名称不能超过255个字符' },
            ]}
          >
            <Input placeholder="例如：2026年11月测评" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

