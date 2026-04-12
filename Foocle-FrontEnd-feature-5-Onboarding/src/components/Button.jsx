import styled from "styled-components";

const FooterBtn = styled.button`
    width: 100%;
    height: ${(props) => props.height};
    text-align: center;
    font-family: "Pretendard-SemiBold";
    font-size: ${(props) => props.fontSize};
    color: ${(props) => (props.reverse == "true" ? "#FFFFFF" : "#FF9B4A")};
    background-color: ${(props) =>
        props.reverse == "true" ? "#FF7300" : "#FFBF8A8C"};
    border: none;
    border-radius: 0.9375rem;
    cursor: pointer;
`;

export const Button = ({ text, onClick, reverse, height, fontSize }) => {
    return (
        <FooterBtn
            onClick={onClick}
            reverse={reverse ? "true" : "false"}
            height={height ? height : "4.375rem"}
            fontSize={fontSize ? fontSize : "1.5rem"}
        >
            {text}
        </FooterBtn>
    );
};