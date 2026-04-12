import styled from 'styled-components';

export const Container = styled.div`
  width: 100%;
  max-width: 600px;
  flex-grow: 1; // 남은 공간 모두 차지
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 2rem;
  border: 1px solid black;
`;
