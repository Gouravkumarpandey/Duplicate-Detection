/// <reference types="vite/client" />

declare global {
  namespace JSX {
    interface IntrinsicElements {
      input: React.DetailedHTMLProps<React.InputHTMLAttributes<HTMLInputElement>, HTMLInputElement> & {
        webkitdirectory?: string;
      };
    }
  }
}
