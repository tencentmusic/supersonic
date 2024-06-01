export type ConfigParametersItem = {
  dataType: string;
  name: string;
  comment: string;
  value: string;
  defaultValue?: string;
  candidateValues: string[];
  description: string;
  require?: boolean;
  placeholder?: string;
};

export type SystemConfig = {
  id: number;
  admin: string;
  admins: string[];
  parameters: ConfigParametersItem[];
};
