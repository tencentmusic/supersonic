export type dependenciesItem = {
  name: string;
  show: {
    includesValue: string[];
  };
  setDefaultValue: Record<string, any>;
};

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
  visible?: boolean;
  dependencies: dependenciesItem[];
  sliderConfig?: {
    start: {
      text: string;
      value: number;
    };
    end: {
      text: string;
      value: number;
    };
    unit: number;
  };
};

export type SystemConfig = {
  id: number;
  admin: string;
  admins: string[];
  parameters: ConfigParametersItem[];
};
