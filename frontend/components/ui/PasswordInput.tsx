"use client";

/**
 * 비밀번호 입력 + 표시 토글(도메인 무지).
 * Input을 합성하고 우측에 눈 아이콘 토글을 둔다.
 */
import { forwardRef, useState, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

interface PasswordInputProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  error?: boolean;
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  function PasswordInput({ error = false, className, ...rest }, ref) {
    const [visible, setVisible] = useState(false);

    return (
      <div className="relative">
        <input
          ref={ref}
          type={visible ? "text" : "password"}
          aria-invalid={error || undefined}
          className={cn(
            "h-10 w-full rounded-md border bg-white px-3 pr-10 text-sm text-zinc-900",
            "placeholder:text-zinc-400",
            "focus-visible:outline-none focus-visible:ring-2",
            error
              ? "border-red-500 focus-visible:ring-red-400"
              : "border-zinc-300 focus-visible:ring-blue-400",
            "disabled:bg-zinc-50 disabled:text-zinc-400",
            className,
          )}
          {...rest}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "비밀번호 숨기기" : "비밀번호 표시"}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-zinc-500 hover:text-zinc-700"
        >
          {visible ? "🙈" : "👁"}
        </button>
      </div>
    );
  },
);
