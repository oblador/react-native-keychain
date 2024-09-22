export const matchLoadInfo = async (
  username: string,
  password: string,
  storage?: string
) => {
  let regexPattern;

  if (!storage) {
    regexPattern = `^Credentials loaded! .*"password":"${password}","username":"${username}"`;
  } else {
    regexPattern = `^Credentials loaded! .*"storage":"${storage}","password":"${password}","username":"${username}"`;
  }

  regexPattern += '.*$';
  const regex = new RegExp(regexPattern);
  await expect(element(by.text(regex))).toBeVisible();
};
