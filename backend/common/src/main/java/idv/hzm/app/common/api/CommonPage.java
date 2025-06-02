package idv.hzm.app.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分頁數據封裝類
 */
public class CommonPage<T> {

  /**
   * 當前頁碼
   */
  private Integer pageNum;
  /**
   * 每頁數量
   */
  private Integer pageSize;
  /**
   * 總頁數
   */
  private Integer totalPage;
  /**
   * 總條數
   */
  private Long total;
  /**
   * 分頁數據
   */
  private List<T> list;

  public Integer getPageNum() {
    return pageNum;
  }

  public void setPageNum(Integer pageNum) {
    this.pageNum = pageNum;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public Integer getTotalPage() {
    return totalPage;
  }

  public void setTotalPage(Integer totalPage) {
    this.totalPage = totalPage;
  }

  public List<T> getList() {
    return list;
  }

  public void setList(List<T> list) {
    this.list = list;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  /**
   * 將SpringData分頁後的list轉為分頁信息
   */
  public static <T> CommonPage<T> restPage(Page<T> pageInfo) {
    CommonPage<T> result = new CommonPage<>();
    result.setTotalPage(pageInfo.getTotalPages());
    result.setPageNum(pageInfo.getNumber());
    result.setPageSize(pageInfo.getSize());
    result.setTotal(pageInfo.getTotalElements());
    result.setList(pageInfo.getContent());
    return result;
  }
}
