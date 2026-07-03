package com.smarteval.print;

import com.smarteval.evaluatoruser.dto.EvaluatorUserListItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrintCredentialService {
    private final String mobileLoginUrl;

    public PrintCredentialService(@Value("${app.frontend.mobile-url}") String mobileLoginUrl) {
        this.mobileLoginUrl = mobileLoginUrl;
    }

    public String renderHtml(List<EvaluatorUserListItem> items) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <title>考评账号打印</title>
                <style>
                body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
                .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
                .card { border: 1px solid #d1d5db; padding: 16px; border-radius: 8px; page-break-inside: avoid; }
                .label { color: #4b5563; font-size: 12px; margin-bottom: 4px; }
                .value { font-size: 16px; margin-bottom: 10px; }
                @media print {
                  body { margin: 0; }
                  .card { break-inside: avoid; }
                }
                </style>
                </head>
                <body>
                <div class="grid">
                """);
        for (EvaluatorUserListItem item : items) {
            html.append("""
                    <section class="card">
                      <div class="label">登录地址</div>
                      <div class="value">%s</div>
                      <div class="label">部门</div>
                      <div class="value">%s</div>
                      <div class="label">用户名</div>
                      <div class="value">%s</div>
                      <div class="label">密码</div>
                      <div class="value">%s</div>
                    </section>
                    """.formatted(
                    escapeHtml(mobileLoginUrl),
                    escapeHtml(item.departmentName()),
                    escapeHtml(item.username()),
                    escapeHtml(item.passwordPlain())
            ));
        }
        html.append("""
                </div>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
