import { create } from 'zustand';

// 이 형식으로 작성
//   const setHeaderConfig = useHeaderStore((state) => state.setHeaderConfig);
//   const resetHeaderConfig = useHeaderStore((state) => state.resetHeaderConfig);

//   useEffect(() => {
//     setHeaderConfig({
//       showBackButton: true,
//       showCloseButton: false,
//       title: '가게 정보 입력',
//       showCompleteButton: true,
//       onComplete: () => alert('완료 클릭!'),
//     });

//     return () => resetHeaderConfig();
//   }, [setHeaderConfig, resetHeaderConfig]);

const useHeaderStore = create((set) => ({
  showBackButton: false, // 뒤로가기 버튼 표시 여부 (true/false)
  showCloseButton: false, // 닫기(X) 버튼 표시 여부
  title: '', // 헤더 중앙 텍스트
  showCompleteButton: false, // 완료 버튼 여부
  onComplete: null, // 버튼 클릭 시 실행 함수

  setHeaderConfig: (
    config // 상태 변경 함수
  ) =>
    set((state) => ({
      ...state,
      ...config,
    })),

  resetHeaderConfig: () =>
    // 상태 초기화 함수
    set({
      showBackButton: false,
      showCloseButton: false,
      title: '',
      showCompleteButton: false,
      onComplete: null,
    }),
}));

export default useHeaderStore;
