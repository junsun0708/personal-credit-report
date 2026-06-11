import { redirect } from "next/navigation";

/**
 * 루트(/) — 리포트 목록으로 위임.
 * 미인증이면 (protected) 가드가 /login으로 리다이렉트한다.
 */
export default function Home() {
  redirect("/reports");
}
