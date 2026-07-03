export type AccessMap = {
  isAdmin: boolean;
  isEvaluator: boolean;
};

export function access(role?: string): AccessMap {
  return {
    isAdmin: role === 'ADMIN',
    isEvaluator: role === 'EVALUATOR'
  };
}
