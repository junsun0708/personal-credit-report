"use client";

/**
 * SCR-01 · 회원가입 (`/signup`).
 *
 * - react-hook-form + zod 검증(이메일·비밀번호 정책·확인 일치).
 * - 409 EMAIL_DUPLICATED → 이메일 필드 하단 에러.
 * - 가입 성공 → 토스트 후 /login 이동.
 */
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { signupSchema, type SignupFormValues } from "@/lib/validators/auth";
import { useAuth } from "@/hooks/useAuth";
import { extractErrorCode, extractErrorMessage } from "@/lib/api/client";
import { useToast } from "@/components/ui/Toast";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { PasswordInput } from "@/components/ui/PasswordInput";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";

export default function SignupPage() {
  const router = useRouter();
  const toast = useToast();
  const { signupMutation } = useAuth();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isValid },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    mode: "onChange",
    defaultValues: { email: "", password: "", passwordConfirm: "" },
  });

  const onSubmit = handleSubmit((values) => {
    signupMutation.mutate(
      { email: values.email, password: values.password },
      {
        onSuccess: () => {
          toast.show("회원가입이 완료되었습니다. 로그인해 주세요.", "success");
          router.replace("/login");
        },
        onError: (error) => {
          // 409 중복 이메일 → 이메일 필드 에러로 매핑
          if (extractErrorCode(error) === "EMAIL_DUPLICATED") {
            setError("email", {
              type: "server",
              message: "이미 사용 중인 이메일입니다.",
            });
          }
        },
      },
    );
  });

  // 필드 매핑 외의 서버 에러(5xx 등)는 공통 배너
  const isFieldMappedError =
    extractErrorCode(signupMutation.error) === "EMAIL_DUPLICATED";
  const serverError =
    signupMutation.isError && !isFieldMappedError
      ? extractErrorMessage(signupMutation.error, "가입 중 오류가 발생했습니다.")
      : null;

  return (
    <div className="rounded-xl border border-zinc-200 bg-white p-8 shadow-sm">
      <div className="mb-6 text-center">
        <p className="text-sm font-medium text-blue-600">신용 리포트 서비스</p>
        <h1 className="mt-1 text-xl font-bold text-zinc-900">회원가입</h1>
      </div>

      <form onSubmit={onSubmit} className="flex flex-col gap-4" noValidate>
        <FormField label="이메일" htmlFor="email" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            error={Boolean(errors.email)}
            {...register("email")}
          />
        </FormField>

        <FormField
          label="비밀번호"
          htmlFor="password"
          error={errors.password?.message}
          description="· 8자 이상  · 영문/숫자/특수문자 조합"
        >
          <PasswordInput
            id="password"
            autoComplete="new-password"
            placeholder="비밀번호"
            error={Boolean(errors.password)}
            {...register("password")}
          />
        </FormField>

        <FormField
          label="비밀번호 확인"
          htmlFor="passwordConfirm"
          error={errors.passwordConfirm?.message}
        >
          <PasswordInput
            id="passwordConfirm"
            autoComplete="new-password"
            placeholder="비밀번호 확인"
            error={Boolean(errors.passwordConfirm)}
            {...register("passwordConfirm")}
          />
        </FormField>

        {serverError && <Alert type="error">{serverError}</Alert>}

        <Button
          type="submit"
          size="lg"
          loading={signupMutation.isPending}
          disabled={!isValid}
          className="w-full"
        >
          가입하기
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-zinc-500">
        이미 계정이 있으신가요?{" "}
        <Link href="/login" className="font-medium text-blue-600 hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}
