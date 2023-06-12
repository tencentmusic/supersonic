const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const commitId = execSync('git rev-parse HEAD').toString().trim();

const file = path.resolve(__dirname, './public/version.js');
const data = {
  commitId: commitId,
  updateTime: new Date().toString(),
};
const feVersion = JSON.stringify(data, null, 4);
// 异步写入数据到文件
fs.writeFile(file, `feVersion=${feVersion}`, { encoding: 'utf8' }, (err) => {});
console.log(`成功写入版本文件，版本信息为${feVersion}`);
