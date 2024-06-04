const { REACT_APP_ENV = 'dev' } = process.env;

const opConfig = {
  domain: 'https://home.cvte.com',
  appId: 'd72f1f3c12904d359e1de7e99cac61e8',
};

const testOpConfig = {
  domain: 'https://op-fat.cvte.com',
  appId: 'b9114441c4544c5093ce5754a9f8b6c4',
};
const OP_CONFIG = REACT_APP_ENV === 'prod' ? opConfig : testOpConfig;
export default OP_CONFIG;
