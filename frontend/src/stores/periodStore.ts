import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface EvaluationPeriod {
  id: number;
  name: string;
}

interface PeriodStore {
  currentPeriod: EvaluationPeriod | null;
  setCurrentPeriod: (period: EvaluationPeriod | null) => void;
  clearCurrentPeriod: () => void;
}

export const usePeriodStore = create<PeriodStore>()(
  persist(
    (set) => ({
      currentPeriod: null,
      setCurrentPeriod: (period) => set({ currentPeriod: period }),
      clearCurrentPeriod: () => set({ currentPeriod: null }),
    }),
    {
      name: 'evaluation-period-storage',
    },
  ),
);
