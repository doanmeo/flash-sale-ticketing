import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertCircle, RotateCcw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Frontend Uncaught Error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#faf9fd] flex items-center justify-center p-6 text-center">
          <div className="max-w-md w-full bg-white rounded-3xl border border-gray-200 shadow-xl p-8">
            <div className="w-14 h-14 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <AlertCircle className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-black text-gray-900 mb-2">Đã xảy ra lỗi giao diện</h2>
            <p className="text-xs text-gray-500 mb-4 leading-relaxed">
              {this.state.error?.message || 'Có lỗi phát sinh trong quá trình hiển thị component.'}
            </p>
            <div className="p-3 bg-gray-50 rounded-xl text-left text-[11px] font-mono text-gray-700 overflow-x-auto max-h-36 mb-6 border border-gray-200">
              {this.state.error?.stack || String(this.state.error)}
            </div>
            <button
              onClick={() => {
                localStorage.clear();
                window.location.href = '/';
              }}
              className="w-full py-3 px-4 bg-brand-500 hover:bg-brand-600 text-white font-bold rounded-xl flex items-center justify-center gap-2 transition shadow-md"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Xóa bộ nhớ đệm & Tải lại trang</span>
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
