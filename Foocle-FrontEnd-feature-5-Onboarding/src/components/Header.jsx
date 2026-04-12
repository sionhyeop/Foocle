// 헤더 바 (뒤로가기 버튼 + 페이지 제목) 담당
// Join/Login, Join/Signup, Curation/... 구현에 사용

import React from 'react';
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';
import useHeaderStore from '../stores/headerStore';
import IconDone from '../../public/img/icon_done.svg';
import XIcon from '../../public/img/icon_x.svg';

const HeaderWrapper = styled.header`
  max-width: 600px;
  height: 3.5rem; /* 56px */
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem; /* 16px */
`;

const Button = styled.button`
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #000;
  min-width: 2.75rem; /* 44px */
  min-height: 2.75rem; /* 44px */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;

  &:active {
    opacity: 0.6;
  }
`;

const CompleteButton = styled(Button)`
  color: var(--Maincolor-1, #ff7300);
  font-family: "Pretendard-Medium"; /* 글씨체 수정 */
  font-size: 1rem; /* 24px */
  font-weight: 500;
  line-height: 2rem; /* 40px */
  font-style: normal;
  text-align: center;
`;

const Title = styled.div`
  flex: 1;
  text-align: center;
  font-weight: 600;
  font-size: 1.125rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 0.75rem;
`;

const ButtonContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 0.5rem; // 8px
`;

export default function Header() {
  const navigate = useNavigate();
  const { showBackButton, showCloseButton, title, showCompleteButton, onComplete } = useHeaderStore();

  return (
    <HeaderWrapper>
      <ButtonContainer>
        {showBackButton && (
          <Button onClick={() => navigate(-1)}>
            <img src={IconDone} alt="뒤로 가기" />
          </Button>
        )}
        {showCloseButton && (
          <Button onClick={() => navigate('/')}>
            <img src={XIcon} alt="닫기" />
          </Button>
        )}
      </ButtonContainer>

      <Title>{title}</Title>

      <ButtonContainer>
        {showCompleteButton && <CompleteButton onClick={onComplete}>완료</CompleteButton>}
      </ButtonContainer>
    </HeaderWrapper>
  );
}
