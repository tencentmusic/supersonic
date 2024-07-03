const { BUILD_ENV = 'dev' } = process.env;

const opConfig = {
  domain: 'https://op.cvte.com',
  appId: 'f141a43a10e244e99e6e8dcd081c6658',
};

const testOpConfig = {
  domain: 'https://op-fat.cvte.com',
  appId: 'b9114441c4544c5093ce5754a9f8b6c4',
};
const OP_CONFIG = BUILD_ENV === 'prod' ? opConfig : testOpConfig;
export default OP_CONFIG;
