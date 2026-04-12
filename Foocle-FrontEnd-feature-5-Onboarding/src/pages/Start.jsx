// 시작 페이지
import styled from "styled-components";
import { Container } from "../styles/Container.style.js";
import LogoText from "../../public/logo_text.svg";
import KakaoLogo from "../../public/img/kakao_logo.svg";

const TitleWrapper = styled.div`
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    margin-top: 20.875rem;
`;
const TitleLogo = styled.img`
    width: 100%;
    max-width: 540px;
`;
const SubtitleSection = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    margin-top: 3.125rem;
`;
const Description = styled.div`
    font-size: 2rem;
    font-family: Pretendard-SemiBold;
`;
const HighlightText = styled(Description)`/* Description 스타일을 상속받아 사용 */
    color: #FF7300;
`;
const BtnWrapper = styled.div`
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1.25rem;
    margin-top: 5.67rem;

    a {
        font-size: 1.125rem;
        font-family: Pretendard-SemiBold;
        color:#4D4D4D;
        padding-top: 0.625rem;
        cursor: pointer;
        text-decoration: none; /* 밑줄 제거 */
    }
`;
const BaseBtn = styled.div`
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 4.375rem;
    border-radius: 0.9375rem;
    cursor: pointer;

    p {
       font-family: Pretendard-Medium;
       font-size: 1.5rem;
    }
`;
const KakaoBtn = styled(BaseBtn)`/* BaseBtn 스타일을 상속받아 사용 */
    background-color: #FAE100;
    gap: 1.25rem;
  
    img {
       width: 1.875rem;
       height: 1.73rem;
    }

    p {
       color: #222222;
    }

    &:hover {
        background-color: #fdef73;
    }
`;
const LoginBtn = styled(BaseBtn)`/* BaseBtn 스타일을 상속받아 사용 */
    background-color: #F0F0F0;

    p {
        color: #222222;
    }
`;
const SignupBtn = styled(BaseBtn)`/* BaseBtn 스타일을 상속받아 사용 */
    background-color: #4D4D4D;

    p {
        color: #FFFFFF;
    }
`;
const Disclaimer = styled.p`
    width: 100%;
    max-width: 27.875rem;
    text-align: center;
    line-height: 1.75rem;
    margin-top: 1.875rem;
    font-size: 1.125rem;
    font-family: "Pretendard-SemiBold";
    color:#D0D0D0;
`;

export const Start = () => {
    return (
        <Container>
            <TitleWrapper>
                <TitleLogo src={LogoText} alt="메인 로고 텍스트" />
                <SubtitleSection>
                    <Description>이미지와 텍스트로 만드는</Description>
                    <HighlightText>AI 쇼츠생성 플랫폼</HighlightText>
                </SubtitleSection>
            </TitleWrapper>
            <BtnWrapper>
                <KakaoBtn>
                    <img src={KakaoLogo} alt="카카오 로그인 로고" />
                    <p>카카오로 3초 만에 시작하기</p>
                </KakaoBtn>
                <LoginBtn>
                    <p>이메일로 로그인하기</p>
                </LoginBtn>
                <SignupBtn>
                    <p>회원가입하기</p>
                </SignupBtn>
                <a>비회원으로 시작하기</a>
            </BtnWrapper>
            <Disclaimer>
                가입을 진행할 경우, 이용약관과 개인정보 수집 및 이용에 대해 동의한 것으로 간주됩니다.
            </Disclaimer>
        </Container>
    );
};