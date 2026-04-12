// 가게 정보 입력 페이지
// 카테고리 이미지는 public/img 폴더 안에 저장

import React from 'react';
import { useEffect } from 'react';
import useHeaderStore from '../../stores/headerStore';
const InputShopInfo = () => {
  const setHeaderConfig = useHeaderStore((state) => state.setHeaderConfig);
  const resetHeaderConfig = useHeaderStore((state) => state.resetHeaderConfig);

  useEffect(() => {
    setHeaderConfig({
      showBackButton: true,
      showCloseButton: false,
      title: '가게 정보 입력',
      showCompleteButton: true,
      onComplete: () => alert('완료 클릭!'),
    });

    return () => resetHeaderConfig();
  }, [setHeaderConfig, resetHeaderConfig]);
  return <div>InputShopInfo</div>;
};

export default InputShopInfo;
