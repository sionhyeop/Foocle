// 로그인 후 메인 레이아웃입니다.

import React from 'react';
import { Outlet } from 'react-router-dom';
import styled from 'styled-components';
import Header from '../components/Header';
import { Container } from '../styles/Container.style';

const LayoutWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh; // 최상위에서 100vh 지정
`;
export default function MainLayout() {
  return (
    <>
      <LayoutWrapper>
        <Header />
        <Container>
          <Outlet />
        </Container>
      </LayoutWrapper>
    </>
  );
}
