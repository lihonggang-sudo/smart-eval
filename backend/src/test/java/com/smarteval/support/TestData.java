package com.smarteval.support;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.department.Department;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatoruser.EvaluatorUser;

public final class TestData {
    private TestData() {
    }

    public static Department department(String name) {
        return new Department(name, 0);
    }

    public static Evaluatee evaluatee(String name, Department department) {
        return new Evaluatee(name, department, true);
    }

    public static EvaluatorUser evaluator(String username, Department department) {
        return new EvaluatorUser(username, "P12345", department, true);
    }

    public static AdminUser admin(String username, String passwordHash) {
        return new AdminUser(username, passwordHash, "管理员", true);
    }
}
