"use client";

/**
 * SCR-02 · 로그인 (`/login`).
 *
 * - react-hook-form + zod 검증.
 * - 로그인 성공 → 토큰/유저 저장 후 returnUrl(없으면 /reports) 이동.
 * - 401은 폼 공통 에러 배너(필드 특정 없이 — 보안).
 */
import { Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { loginSchema, type LoginFormValues } from "@/lib/validators/auth";
import { useAuth } from "@/hooks/useAuth";
import { extractErrorMessage } from "@/lib/api/client";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { PasswordInput } from "@/components/ui/PasswordInput";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { loginMutation } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: "onChange",
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit((values) => {
    loginMutation.mutate(values, {
      onSuccess: () => {
        const returnUrl = searchParams.get("returnUrl");
        router.replace(returnUrl && returnUrl.startsWith("/") ? returnUrl : "/reports");
      },
    });
  });

  // 401 등 서버 에러 메시지(공통 배너)
  const serverError = loginMutation.isError
    ? extractErrorMessage(
        loginMutation.error,
        "이메일 또는 비밀번호가 올바르지 않습니다.",
      )
    : null;

  return (
    <div className="rounded-xl border border-zinc-200 bg-white p-8 shadow-sm">
      <div className="mb-6 text-center">
        <p className="text-sm font-medium text-blue-600">신용 리포트 서비스</p>
        <h1 className="mt-1 text-xl font-bold text-zinc-900">로그인</h1>
      </div>

      <form onSubmit={onSubmit} className="flex flex-col gap-4" noValidate>
        <FormField label="이메일" htmlFor="email" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="test@kcs.co.kr"
            error={Boolean(errors.email)}
            {...register("email")}
          />
        </FormField>

        <FormField label="비밀번호" htmlFor="password" error={errors.password?.message}>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            placeholder="비밀번호"
            error={Boolean(errors.password)}
            {...register("password")}
          />
        </FormField>

        {serverError && <Alert type="error">{serverError}</Alert>}

        <Button
          type="submit"
          size="lg"
          loading={loginMutation.isPending}
          disabled={!isValid}
          className="w-full"
        >
          로그인
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-zinc-500">
        계정이 없으신가요?{" "}
        <Link href="/signup" className="font-medium text-blue-600 hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  );
}

export default function LoginPage() {
  // useSearchParams 사용으로 Suspense 경계 필요
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
