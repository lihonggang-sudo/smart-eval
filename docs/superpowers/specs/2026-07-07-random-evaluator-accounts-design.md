# Random Evaluator Accounts Design

## Context

The admin evaluator user page currently supports batch account generation. The backend creates usernames with a department-derived prefix and an incrementing sequence, such as `D003-0001`. This makes accounts predictable and visibly ordered.

The required change is to keep batch generation, but make generated usernames completely random while still guaranteeing uniqueness.

## Requirements

- Batch-generated evaluator usernames must be fully random.
- Usernames must not include department IDs, department codes, or increasing sequence numbers.
- Usernames must remain unique across all evaluator users.
- Usernames created in the same batch must also be unique before saving.
- Initial passwords keep the current behavior: 6 uppercase alphanumeric characters.
- The existing batch generation UI and request shape stay the same: admin selects a department and count.
- Generated users still belong to the selected department.

## Username Format

Generated evaluator usernames will be 8 uppercase alphanumeric characters using this character set:

```text
ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
```

Examples:

```text
K7P4Q2XA
M9Z0BD4R
```

Eight characters provide a large candidate space while keeping the username practical for printed credentials and manual login.

## Backend Design

`RandomCredentialGenerator` will own random credential string generation for both usernames and passwords.

It will expose a random username generator that returns one 8-character uppercase alphanumeric username at a time. Password generation remains 6 characters.

`EvaluatorUserService.batchGenerate` will:

1. Validate admin access and department existence as it does today.
2. Generate random username candidates until it has `count` unique usernames.
3. Reject a candidate if it already appears in the current batch.
4. Reject a candidate if `EvaluatorUserRepository.existsByUsername(candidate)` returns true.
5. Create users for the selected department with the accepted usernames and generated passwords.
6. Save the batch and return the existing response shape.

To avoid an infinite loop in an extreme collision scenario, the service will cap attempts. A practical cap is `count * 20`, with a minimum floor such as `100`. If the service cannot create enough unique usernames within the cap, it returns a business error such as `生成账号失败，请重试`.

The database remains the final uniqueness guard through the existing unique username constraint.

## Frontend Design

No UI change is required. The table will naturally display the random username returned by the backend. The batch generation modal still asks for department and count because department membership is still required even though the username no longer contains department information.

## Error Handling

- Validation errors for count and department ID remain unchanged.
- Missing department still returns `部门不存在`.
- If random generation cannot produce enough unique usernames within the retry cap, return a business error with a clear retry message.
- If a database unique constraint race occurs despite pre-checking, the request may fail through existing persistence error handling. The random space and pre-checks make this unlikely; no new distributed locking is required for this scope.

## Testing

Backend tests should be updated to cover:

- `RandomCredentialGenerator` creates 8-character uppercase alphanumeric usernames.
- A generated batch returns unique usernames.
- Generated usernames do not match the old sequential department format.
- Generated users still use the selected department.
- Passwords remain 6-character uppercase alphanumeric strings.
- Existing validation tests for minimum and maximum count continue to pass.

Frontend tests do not need new behavior checks unless existing snapshots or assertions assume the old username format.
