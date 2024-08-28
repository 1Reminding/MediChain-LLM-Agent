import sys

from dotenv import load_dotenv, find_dotenv
from langchain_community.document_loaders import CSVLoader  # 文档加载器，采用csv格式存储
from langchain_community.vectorstores import DocArrayInMemorySearch  # 向量存储
from langchain_community.embeddings import BaichuanTextEmbeddings
import os
from langchain_core.prompts import PromptTemplate
from langchain_community.chat_message_histories import ChatMessageHistory
from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_core.runnables import RunnablePassthrough
from langchain_core.messages import HumanMessage
from langchain_core.messages import AIMessage
from langchain_openai import ChatOpenAI
import pymysql
from langchain import hub
from langchain.chains import create_history_aware_retriever
from langchain_core.prompts import MessagesPlaceholder
from langchain_chroma import Chroma
from langchain_core.output_parsers import StrOutputParser
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain import hub
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_chroma import Chroma
from langchain_community.document_loaders import WebBaseLoader
from langchain_core.prompts import ChatPromptTemplate
from langchain_text_splitters import RecursiveCharacterTextSplitter

_ = load_dotenv(find_dotenv())  # 读取环境变量


def filter_messages(messages, k=10):
    return messages[-k:]


# 初始化数据库连接
def initialize_db():
    conn = pymysql.connect(host="localhost", user="newuser", password="newpassword", db="chatAI", port=3306)
    cursor = conn.cursor()
    cursor.execute('''CREATE TABLE IF NOT EXISTS chat_history (
                         session_id VARCHAR(255),
                         message_type VARCHAR(255),
                         content TEXT
                     )''')
    conn.commit()
    cursor.close()
    conn.close()


initialize_db()


# 从数据库获取会话历史记录
def get_session_history_from_db(session_id: str, k=10) -> BaseChatMessageHistory:
    conn = pymysql.connect(host="localhost", user="newuser", password="newpassword", db="chatAI", port=3306)
    cursor = conn.cursor()
    cursor.execute("SELECT message_type, content FROM chat_history WHERE session_id = %s ORDER BY id ASC",
                   (session_id,))
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    history = ChatMessageHistory()
    for row in rows:
        if row[0] == 'human':
            history.add_message(HumanMessage(content=row[1]))
        elif row[0] == 'ai':
            history.add_message(AIMessage(content=row[1]))

    # 过滤消息
    history.messages = filter_messages(history.messages, k)
    return history


# 将会话历史记录存入数据库
def save_session_history_to_db(session_id: str, message_type: str, content: str):
    conn = pymysql.connect(host="localhost", user="newuser", password="newpassword", db="chatAI", port=3306)
    cursor = conn.cursor()
    cursor.execute("INSERT INTO chat_history (session_id, message_type, content) VALUES (%s, %s, %s)",
                   (session_id, message_type, content))
    conn.commit()
    cursor.close()
    conn.close()


# 使用字典存储会话历史
store = {}


def get_record_count(session_id: str):
    conn = pymysql.connect(host="localhost", user="newuser", password="newpassword", db="chatAI", port=3306)
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(session_id) FROM chat_history WHERE session_id = %s", (session_id,))
    count = cursor.fetchone()[0]
    cursor.close()
    conn.close()
    return count


def get_session_history(session_id: str) -> BaseChatMessageHistory:
    if session_id not in store:
        store[session_id] = get_session_history_from_db(session_id)
    return store[session_id]


def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)


config = {
    "configurable": {
        "session_id": "a5"
    }
}

model = ChatOpenAI(
    base_url="https://open.bigmodel.cn/api/paas/v4",
    api_key=os.environ["ZHIPUAI_API_KEY"],
    model="glm-4",
)

# 包装模型，使用消息历史记录
with_message_history = RunnableWithMessageHistory(
    model,
    get_session_history
)


def query(context):
    contextualize_q_system_prompt = (
        "你是一位智慧医生，可以根据聊天记录中患者对自己病情的描述及用户对自己病情的最新描述"
        "该问题可能涉及聊天记录中的上下文信息，重新构思一个独立的问题，"
        "使其能够了解更多有关于患者的病情信息"
        "不要直接回答问题，而是在需要时对其进行重新表述，否则直接按原样返回问题。"
        "回答的格式如下：请问+想要询问的信息"
        "询问的问题优先从持续时间，主要症状特点，发作原因里选择，但是只询问你不知道的信息"
        "不要输出问题之外的任何部分，且最多询问一个问题"
        "提问时温和一些，带一些人文关怀"
    )

    contextualize_q_prompt = ChatPromptTemplate.from_messages([
        ("system", contextualize_q_system_prompt),
        ("human", "{input}")
    ])
    PROMPT = contextualize_q_prompt.format(input=context)

    # 调用模型并传递配置
    resp = with_message_history.invoke(
        PROMPT,
        config=config
    )
    return resp


def judge(context):
    prompt_template = """根据上下文中用户所有对病情的描述，判断你是否可以给出一个较为准确的判断。
                       如果能准确判断，病情描述至少包含以下内容如持续时间，主要症状特点，发作原因。
                       不要用多个词回答，只需用一个字是或否回答问题，其他部分一律不出现             
                        <question>
                       {question}
                       </question>             
                        分类："""

    PROMPT = prompt_template.format(question=context)

    # 调用模型并传递配置
    resp = with_message_history.invoke(
        PROMPT,
        config=config
    )
    return resp.content


def diagnose(context):
    # 加载药品数据集
    csv_loader = CSVLoader(file_path="E:\medicine\pythonProject\medicine.csv", encoding='utf8')
    docs = csv_loader.load()

    # 将数据集拆分成小块
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000, chunk_overlap=200, add_start_index=True
    )
    all_splits = text_splitter.split_documents(docs)

    # 创建嵌入并向量化数据集
    embeddings = BaichuanTextEmbeddings(baichuan_api_key=os.environ["BAICHUAN_API_KEY"])
    db = DocArrayInMemorySearch.from_documents(
        all_splits,
        embeddings
    )

    # 创建检索器
    retriever = db.as_retriever(search_type="similarity", search_kwargs={"k": 6})
    contextualize_q_system_prompt = (
        "你是一位智慧医生，根据上下文患者对病情描述生成患者的诊断报告和诊断建议"
        "诊断报告包括对患者病情的描述，诊断建议是对患者的养病建议"
        "如果你不知道答案，就说你不知道。"
    )

    contextualize_q_prompt = ChatPromptTemplate.from_messages([
        ("system", contextualize_q_system_prompt),
        ("human", "{input}")
    ])
    PROMPT = contextualize_q_prompt.format(input=context)

    # 调用模型并传递配置
    resp1 = with_message_history.invoke(
        PROMPT,
        config=config
    )
    diagnosis = resp1.content

    # 定义诊断报告生成和药品推荐的提示模板
    system_prompt = (
        "你是一位智慧医生，根据患者对病情描述和以下检索到的药品信息进行药品推荐。"
        "药品推荐包含药品的功能，服用方法和注意事项，列出两个最合适的药品即可"
        "回答格式为：以下是为您推荐的的药品："
        "\n\n"
        "{context}"
    )

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human", "{input}")
    ])

    # 创建诊断报告生成和药品推荐的链
    question_answer_chain = create_stuff_documents_chain(model, prompt)
    rag_chain = create_retrieval_chain(retriever, question_answer_chain)

    resp2 = rag_chain.invoke(
        {"input": diagnosis}
    )
    drug = resp2["answer"]
    resp = diagnosis + '\n' + drug
    return resp


def agent(context):
    session_id = config["configurable"]["session_id"]

    # 保存用户输入到数据库
    save_session_history_to_db(session_id, 'human', context)
    if get_record_count(session_id) >= 6:
        # 如果循环次数达到最大值，仍然无法判断，生成一个基础的诊断报告
        report = diagnose(context)  # 假设 diagnose 函数会返回诊断报告的内容
        save_session_history_to_db(session_id, 'ai', report)
        return report
    # 判断是否可以给出诊断
    judgment = judge(context)

    if '是' in judgment:
        # 调用诊断报告生成函数
        report = diagnose(context)  # 假设 diagnose 函数会返回诊断报告的内容
        save_session_history_to_db(session_id, 'ai', report)
        return report
    elif '否' in judgment:
        # 向患者询问更多信息
        report = query(context)
        context = report.content
        save_session_history_to_db(session_id, 'ai', report.content)
        return context


if __name__ == "__main__":
    try:
        context = "身体不舒服"  # 默认输入以防止没有参数时出现错误
        if len(sys.argv) > 1:
            context = sys.argv[1]
        resp = agent(context)
        print(resp)  # 直接打印字符串内容
    except Exception as e:
        print(f"Error: {str(e)}")
