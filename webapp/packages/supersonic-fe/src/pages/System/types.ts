export type SystemConfigParametersItem = {
  dataType: string;
  name: string;
  comment: string;
  value: string;
  candidateValues: string[];
  description: string;
};

export type SystemConfig = {
  id: number;
  admin: string;
  admins: string[];
  parameters: SystemConfigParametersItem[];
};
