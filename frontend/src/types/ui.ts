export type FormMessage = {
  type: "info" | "error";
  text: string;
};

export type HealthCheckStatus = "checking" | "ok" | "error";

export type HealthCheckState = {
  status: HealthCheckStatus;
  message: string;
  checkedAt: string | null;
};
