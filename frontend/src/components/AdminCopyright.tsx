export const ADMIN_COPYRIGHT_TEXT = 'Copyright © 2026 鲁东大学 领导干部试用期满民主测评系统. All Rights Reserved.';

export default function AdminCopyright() {
  return (
    <footer
      data-testid="admin-copyright"
      style={{
        color: '#8c8c8c',
        fontSize: 12,
        lineHeight: 1.6,
        padding: '18px 24px 22px',
        textAlign: 'center'
      }}
    >
      {ADMIN_COPYRIGHT_TEXT}
    </footer>
  );
}
