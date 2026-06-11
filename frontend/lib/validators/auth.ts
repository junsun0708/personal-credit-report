/**
 * 인증 폼 zod 스키마.
 * 비밀번호 정책은 백엔드와 미러링: 8자 이상 + 영문/숫자/특수문자 조합.
 */
import { z } from "zod";

/** 비밀번호 정책 정규식: 영문·숫자·특수문자 각 1자 이상 */
const PASSWORD_PATTERN =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/;

const emailSchema = z
  .string()
  .min(1, "이메일을 입력해 주세요.")
  .email("올바른 이메일 형식이 아닙니다.");

const passwordSchema = z
  .string()
  .min(8, "비밀번호는 8자 이상이어야 합니다.")
  .regex(PASSWORD_PATTERN, "영문/숫자/특수문자를 모두 포함해야 합니다.");

/** 로그인 폼 스키마 (정책 검증은 로그인 시 노출하지 않음 — 공통 에러로 처리) */
export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, "비밀번호를 입력해 주세요."),
});

/** 회원가입 폼 스키마 (비밀번호 확인 일치 검증 포함) */
export const signupSchema = z
  .object({
    email: emailSchema,
    password: passwordSchema,
    passwordConfirm: z.string().min(1, "비밀번호 확인을 입력해 주세요."),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: "비밀번호가 일치하지 않습니다.",
    path: ["passwordConfirm"],
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;
