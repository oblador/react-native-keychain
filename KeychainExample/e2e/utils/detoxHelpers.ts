export const ResetDevice = async () => {
  await device.launchApp({ delete: true, newInstance: true });
};
