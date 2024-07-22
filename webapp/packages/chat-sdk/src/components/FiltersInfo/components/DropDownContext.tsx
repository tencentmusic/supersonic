import { createContext, useContextSelector } from 'use-context-selector';

type Props = {
  value: (value: boolean) => void;
};

const DropDownContext = createContext<{
  handleDropdown: (value: boolean) => void;
}>({
  handleDropdown: () => {},
});

export const useDropDownHandler = () => {
  const handleDropdown = useContextSelector(DropDownContext, context => context.handleDropdown);
  return handleDropdown;
};

export default function DropDownHandlerProvider({
  value: handleDropdown,
  children,
}: Props & { children: React.ReactNode }) {
  return <DropDownContext.Provider value={{ handleDropdown }}>{children}</DropDownContext.Provider>;
}
